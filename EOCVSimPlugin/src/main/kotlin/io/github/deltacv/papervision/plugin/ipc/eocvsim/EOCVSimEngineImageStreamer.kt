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

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.util.MovingStatistics
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.papervision.engine.PaperVisionEngine
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.util.ReusableBufferPool
import io.github.deltacv.papervision.util.loggerFor
import io.github.deltacv.vision.external.util.extension.aspectRatio
import io.github.deltacv.vision.external.util.extension.clipTo
import org.deltacv.mackjpeg.MackJPEG
import org.deltacv.mackjpeg.PixelFormat
import org.deltacv.mackjpeg.exception.JPEGException
import org.libjpegturbo.turbojpeg.TJ
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.math.roundToInt

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
                logger.error("No JPEG backend is available for MackJPEG! Image streaming will not work!")
            } else {
                logger.info("Using JPEG backend: ${MackJPEG.getSupportedBackend()!!.name}")
            }
        }
    }

    private val ids = mutableMapOf<Int, Long>()

    private val latestMatMap = mutableMapOf<Int, Mat>()
    private val maskMatMap = mutableMapOf<Int, Mat>()

    private val changeRateAvgs = mutableMapOf<Int, MovingStatistics>()
    private val changeRateTimers = mutableMapOf<Int, ElapsedTime>()
    private val diffSlowDownTimers = mutableMapOf<Int, ElapsedTime>()
    private val hasSent = mutableMapOf<Int, Boolean>()

    private val changeCheckLock = mutableMapOf<Int, Any>()

    private val bufferPool = ReusableBufferPool(5)
    private val matRecycler = MatRecycler(10)

    private val reportedJpegExceptions = mutableSetOf<String>()
    private val reportedJpegExceptionsCleanup = ElapsedTime()

    val tag by lazy { ByteMessageTag.fromString(previzNameProvider()) }

    private val jpegWorkers = Executors.newFixedThreadPool(5) { r ->
        val t = Thread(r)
        t.isDaemon = true
        t.name = "JPEG-Comp-Worker-${t.id}"

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

        try {
            submitJpeg(id, targetImage)
        } catch (_: RejectedExecutionException) {
            // ignored, shutdown in progress
        }
    }

    private fun submitJpeg(id: Int, targetImage: MatRecycler.RecyclableMat) {
        jpegWorkers.submit {
            try {
                (MackJPEG.getSupportedBackend()?.makeCompressor() ?: return@submit).use { compressor ->
                    if (!hasChanged(id, targetImage)) {
                        // no significant changes, skip frame
                        return@submit
                    }

                    if (targetImage.type() != CvType.CV_8UC3) {
                        // convert to 8UC3 to keep turbojpeg happy
                        targetImage.convertTo(targetImage, CvType.CV_8UC3)
                    }

                    // resize
                    scaleToFit(targetImage, targetImage)

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
                        TJ.bufSize(width, height, TJ.SAMP_420), // hope this is enough
                        ReusableBufferPool.MemoryBehavior.ALLOCATE_WHEN_EXHAUSTED
                    ) ?: return@submit

                    try {
                        try {
                            compressor.compress(jpegBuffer)
                        } catch (e: JPEGException) {
                            // only report once, to avoid spamming the logs
                            // report every 5 seconds that "the issue is still present"
                            if (reportedJpegExceptions.add(e.message ?: "unknown")) {
                                logger.error("MackJPEG compression error, falling back to OpenCV for id=$id: ${e.message}", e)
                            } else if (reportedJpegExceptionsCleanup.seconds() >= 5.0) {
                                reportedJpegExceptionsCleanup.reset()
                                logger.warn("MackJPEG compression error still present for id=$id: ${e.message}")
                            }

                            // fallback to opencv !?
                            val bytes = MatOfByte()

                            Imgcodecs.imencode(".jpg", targetImage, bytes)
                            bytes.get(0, 0, jpegBuffer)

                            bytes.release()
                        }

                        // offset jpeg data to leave space for the header
                        System.arraycopy(jpegBuffer, 0, jpegBuffer, 4 + tag.content.size + 4, compressor.compressedSize)

                        // append header to jpegBuffer, uses a ByteBuffer for convenience
                        val byteMessageBuffer = ByteBuffer.wrap(jpegBuffer)

                        byteMessageBuffer.putInt(tag.content.size) // tag size
                        byteMessageBuffer.put(tag.content) // tag
                        byteMessageBuffer.putInt(id) // id
                        // data is already in place thanks to the System.arraycopy above

                        ipcEngine.sendBytes(jpegBuffer)

                        synchronized(ids) {
                            if (!ids.containsKey(id) || System.currentTimeMillis() - ids[id]!! > 5000) {
                                val status = if(ids.containsKey(id))
                                    "Started"
                                else "Resumed"

                                val lastFrameInfo = if (ids.containsKey(id))
                                    "${(System.currentTimeMillis() - ids[id]!!) / 1000.0}s ago"
                                else "never received"

                                logger.info("$status streaming for ${previzNameProvider()} id=$id (last frame was $lastFrameInfo)")
                            }

                            ids[id] = System.currentTimeMillis()
                        }
                    } finally {
                        bufferPool.returnBuffer(jpegBuffer)
                    }
                }
            } finally {
                matRecycler.returnMat(targetImage)
            }
        }
    }

    private fun hasChanged(id: Int, image: Mat): Boolean {
        val lock = changeCheckLock.getOrPut(id) { Any() }

        synchronized(lock) {
            if (!hasSent.getOrDefault(id, false)) {
                hasSent[id] = true
                return true // always send the first frame
            }

            val maskMat = maskMatMap.getOrPut(id) { Mat() }
            val latestMat = latestMatMap.getOrPut(id) { Mat() }

            val changeRateTimer = changeRateTimers.getOrPut(id) { ElapsedTime() }
            val diffSlowDownTimer = diffSlowDownTimers.getOrPut(id) { ElapsedTime() }
            val changeRateAvg = changeRateAvgs.getOrPut(id) { MovingStatistics(50) }

            changeRateAvg.add(changeRateTimer.seconds())

            val mean = changeRateAvg.mean
            val isFastChange = mean <= 0.5

            val forceSend = changeRateTimer.seconds() >= 5.0

            // Perform the diff check only if the change rate is slow enough
            try {
                // Skip diff check if changes are frequent
                if (isFastChange || forceSend) {
                    return true
                } else if (!latestMat.empty() && latestMat.size() == image.size()) {
                    // Slow down diff check after 3 seconds of no changes
                    if (changeRateTimer.seconds() > 3) {
                        val slowDown = ((changeRateTimer.seconds() - 3) * 0.1).coerceAtMost(0.5)
                        if (diffSlowDownTimer.seconds() <= slowDown) {
                            return false
                        } else {
                            diffSlowDownTimer.reset()
                        }
                    }

                    Core.absdiff(latestMat, image, maskMat)
                    Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_RGB2GRAY)

                    val diffPixels = Core.countNonZero(maskMat)

                    // If there are significant differences or change rate indicates slower changes, send bytes
                    if (diffPixels > 0) {
                        changeRateTimer.reset()
                        return true
                    } else {
                        return false
                    }
                } else {
                    return true // Size changed, send frame
                }
            } catch (e: Exception) {
                logger.error("hasChanged exception for stream id $id", e)
                return true // In case of error, send the frame
            } finally {
                image.copyTo(latestMat) // Update the latest mat
            }
        }
    }

    private fun scaleToFit(src: Mat, dst: Mat) {
        if (src.size() == resolution) { //nice, the mat size is the exact same as the video size
            if (src != dst) src.copyTo(dst)
        } else { //uh oh, this might get a bit harder here...
            val targetR = resolution.aspectRatio()
            val inputR = src.aspectRatio()

            //ok, we have the same aspect ratio, we can just scale to the required size
            if (targetR == inputR) {
                Imgproc.resize(src, dst, resolution, 0.0, 0.0, Imgproc.INTER_AREA)
            } else { //hmm, not the same aspect ratio, we'll need to do some fancy stuff here...
                val inputW = src.size().width
                val inputH = src.size().height

                val widthRatio = resolution.width / inputW
                val heightRatio = resolution.height / inputH
                val bestRatio = widthRatio.coerceAtMost(heightRatio)

                val newSize = Size(inputW * bestRatio, inputH * bestRatio).clipTo(resolution)

                //get offsets so that we center the image instead of leaving it at (0,0)
                //(basically the black bars you see)
                val xOffset = (resolution.width - newSize.width) / 2
                val yOffset = (resolution.height - newSize.height) / 2

                val resizedImg = matRecycler.takeMatOrNull()

                try {
                    resizedImg.create(newSize, src.type())
                    Imgproc.resize(src, resizedImg, newSize, 0.0, 0.0, Imgproc.INTER_AREA)

                    dst.create(resolution, src.type())
                    dst.setTo(Scalar(0.0, 0.0, 0.0, 255.0))

                    //get submat of the exact required size and offset position from the "videoMat",
                    //which has the user-defined size of the current video.
                    val rectX = xOffset.roundToInt().coerceAtLeast(0)
                    val rectY = yOffset.roundToInt().coerceAtLeast(0)
                    val rectWidth = newSize.width.roundToInt()
                    val rectHeight = newSize.height.roundToInt()

                    val submat = dst.submat(Rect(rectX, rectY, rectWidth, rectHeight))

                    //then we copy our adjusted mat into the gotten submat. since a submat is just
                    //a reference to the parent mat, when we copy here our data will be actually
                    //copied to the actual mat, and so our new mat will be of the correct size and
                    //centered with the required offset
                    resizedImg.copyTo(submat)
                } catch (e: Exception) {
                    logger.error("scaleToFit error", e)
                } finally {
                    resizedImg.returnMat()
                }
            }
        }
    }

    fun stop() {
        logger.info("Stopping EOCVSimEngineImageStreamer")
        jpegWorkers.shutdown()
    }

    fun refreshed() {
        ids.clear()
    }
}