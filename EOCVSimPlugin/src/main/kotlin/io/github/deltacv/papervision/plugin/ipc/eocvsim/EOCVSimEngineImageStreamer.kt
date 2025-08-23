/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.plugin.ipc.eocvsim

import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.papervision.engine.PaperVisionEngine
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.util.ReusableBufferPool
import io.github.deltacv.papervision.util.loggerFor
import org.deltacv.mackjpeg.MackJPEG
import org.deltacv.mackjpeg.PixelFormat
import org.deltacv.mackjpeg.exception.JPEGException
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class EOCVSimEngineImageStreamer(
    val previzNameProvider: () -> String,
    val resolution: Size,
    val ipcEngine: PaperVisionEngine,
    var streamQualityFormula: (Int) -> Int = { 50 }
) : ImageStreamer {

    companion object {
        val logger by loggerFor<EOCVSimEngineImageStreamer>()

        init {
            if (MackJPEG.getSupportedBackend() == null) {
                logger.error("No JPEG backend is available for MackJPEG! EOCVSim image streaming will not work!")
            } else {
                logger.info("Using JPEG backend: ${MackJPEG.getSupportedBackend()!!.name}")
            }
        }
    }

    private val bufferPool = ReusableBufferPool(5)
    private val matRecycler = MatRecycler(10)

    val tag by lazy { ByteMessageTag.fromString(previzNameProvider()) }

    private val jpegWorkers = Executors.newFixedThreadPool(5) { r ->
        val t = Thread(r)
        t.isDaemon = true
        t.name = "EOCVSim-JPEG-Worker-${t.id}"

        t
    }

    override fun sendFrame(
        id: Int,
        image: Mat,
        cvtCode: Int?
    ) {
        if (image.empty()) {
            return
        }
        if (jpegWorkers.isShutdown) {
            return
        }

        val targetImage = matRecycler.takeMatOrNull() ?: return

        if (cvtCode != null) {
            // convert image to the desired color space
            Imgproc.cvtColor(image, targetImage, cvtCode)
        } else {
            image.copyTo(targetImage)
        }

        jpegWorkers.submit {
            try {
                (MackJPEG.getSupportedBackend()?.makeCompressor() ?: return@submit).use { compressor ->
                    // resize
                    if (targetImage.size() != resolution) {
                        // hopefully this is not too slow
                        Imgproc.resize(targetImage, targetImage, resolution)
                    }

                    if (targetImage.type() != CvType.CV_8UC3) {
                        // convert to 8UC3 to keep turbojpeg happy
                        targetImage.convertTo(targetImage, CvType.CV_8UC3)
                    }

                    val imageBuffer = bufferPool.getOrCreate(
                        targetImage.rows() * targetImage.cols() * 3, // width * height * 3 (RGB)
                        ReusableBufferPool.MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
                    ) ?: return@submit

                    // memcpy the mat data to the imageBuffer array
                    targetImage.get(0, 0, imageBuffer)

                    val width = targetImage.cols()
                    val height = targetImage.rows()

                    compressor.setImage(imageBuffer, width, height, PixelFormat.RGB)
                    compressor.setQuality(streamQualityFormula(id).coerceIn(1, 100))

                    val jpegBuffer = bufferPool.getOrCreate(
                        50_000, // 50 KB should be enough for all cases, use common size to recycle buffers better
                        ReusableBufferPool.MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
                    ) ?: return@submit

                    try {
                        try {
                            compressor.compress(jpegBuffer)
                        } catch (e: JPEGException) {
                            logger.error("Failed to compress image for EOCVSim stream", e)
                            return@submit
                        }

                        // offset jpeg data to leave space for the header
                        System.arraycopy(jpegBuffer, 0, jpegBuffer, 4 + tag.tag.size + 4, compressor.compressedSize)

                        // append header to jpegBuffer, uses a ByteBuffer for convenience
                        val byteMessageBuffer = ByteBuffer.wrap(jpegBuffer)

                        byteMessageBuffer.putInt(tag.tag.size) // tag size
                        byteMessageBuffer.put(tag.tag) // tag
                        byteMessageBuffer.putInt(id) // id
                        // data is already in place thanks to the System.arraycopy above

                        ipcEngine.sendBytes(jpegBuffer)
                    } finally {
                        bufferPool.returnBuffer(jpegBuffer)
                    }
                }
            } finally {
                matRecycler.returnMat(targetImage)
            }
        }
    }

    fun stop() {
        logger.info("Stopping EOCVSimEngineImageStreamer")
        jpegWorkers.shutdown()
    }
}