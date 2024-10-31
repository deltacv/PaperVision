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

import com.github.serivesmejia.eocvsim.gui.util.ThreadedMatPoster
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.qualcomm.robotcore.util.MovingStatistics
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.papervision.engine.PaperVisionEngine
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.vision.external.util.extension.aspectRatio
import io.github.deltacv.vision.external.util.extension.clipTo
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import java.lang.Thread.sleep

import java.util.concurrent.ArrayBlockingQueue

class EOCVSimEngineImageStreamer(
    val engine: PaperVisionEngine,
    val tag: String,
    resolution: Size
) : ImageStreamer {

    companion object {
        const val QUEUE_SIZE = 5
    }

    private val poster = ThreadedMatPoster("EOCVSimEngineImageStreamer-Poster", QUEUE_SIZE)
    private val matDataQueue = EvictingBlockingQueue(ArrayBlockingQueue<MatData>(QUEUE_SIZE))

    private val queuesLock = Any()

    private val matRecycler = MatRecycler(3)
    private var bytes = ByteArray(resolution.width.toInt() * resolution.height.toInt() * 3)

    private val latestMatMap = mutableMapOf<Int, Mat>()
    private val maskMatMap = mutableMapOf<Int, Mat>()

    private val changeRateAvgs = mutableMapOf<Int, MovingStatistics>()
    private val changeRateTimers = mutableMapOf<Int, ElapsedTime>()
    private val diffSlowDownTimers = mutableMapOf<Int, ElapsedTime>()

    var resolution = resolution
        set(value) {
            field = value
            bytes = ByteArray(value.width.toInt() * value.height.toInt() * 3)
        }

    val byteTag = ByteMessageTag.fromString(tag)

    val logger by loggerForThis()

    init {
        poster.addPostable(this::processFrame)
    }

    override fun sendFrame(
        id: Int,
        image: Mat,
        cvtCode: Int?
    ) {
        synchronized(queuesLock) {
            poster.post(image)
            matDataQueue.add(MatData(id, cvtCode))
        }
    }

    private fun processFrame(image: Mat) {
        if(image.empty()) return

        val matData = synchronized(queuesLock) { matDataQueue.poll() ?: return }
        val id = matData.id
        val cvtCode = matData.cvtCode

        val scaledImg = matRecycler.takeMatOrNull()

        scaledImg.create(resolution, image.type())
        scaledImg.setTo(Scalar(0.0, 0.0, 0.0))

        try {
            if (image.size() == resolution) { //nice, the mat size is the exact same as the video size
                image.copyTo(scaledImg)
            } else { //uh oh, this might get a bit harder here...
                val targetR = resolution.aspectRatio()
                val inputR = image.aspectRatio()

                //ok, we have the same aspect ratio, we can just scale to the required size
                if (targetR == inputR) {
                    Imgproc.resize(image, scaledImg, resolution, 0.0, 0.0, Imgproc.INTER_AREA)
                } else { //hmm, not the same aspect ratio, we'll need to do some fancy stuff here...
                    val inputW = image.size().width
                    val inputH = image.size().height

                    val widthRatio = resolution.width / inputW
                    val heightRatio = resolution.height / inputH
                    val bestRatio = widthRatio.coerceAtMost(heightRatio)

                    val newSize = Size(inputW * bestRatio, inputH * bestRatio).clipTo(resolution)

                    //get offsets so that we center the image instead of leaving it at (0,0)
                    //(basically the black bars you see)
                    val xOffset = (resolution.width - newSize.width) / 2
                    val yOffset = (resolution.height - newSize.height) / 2

                    val resizedImg = matRecycler.takeMatOrNull()
                    resizedImg.create(newSize, image.type())

                    try {
                        Imgproc.resize(image, resizedImg, newSize, 0.0, 0.0, Imgproc.INTER_AREA)

                        //get submat of the exact required size and offset position from the "videoMat",
                        //which has the user-defined size of the current video.
                        val submat = scaledImg.submat(Rect(Point(xOffset, yOffset), newSize))

                        //then we copy our adjusted mat into the gotten submat. since a submat is just
                        //a reference to the parent mat, when we copy here our data will be actually
                        //copied to the actual mat, and so our new mat will be of the correct size and
                        //centered with the required offset
                        resizedImg.copyTo(submat)
                    } finally {
                        resizedImg.returnMat()
                    }
                }
            }

            if(cvtCode != null) {
                Imgproc.cvtColor(scaledImg, scaledImg, cvtCode)
            }

            if(scaledImg.type() == CvType.CV_8UC1) {
                Imgproc.cvtColor(scaledImg, scaledImg, Imgproc.COLOR_GRAY2RGB)
            } else if(scaledImg.type() != CvType.CV_8UC3) {
                throw IllegalArgumentException("Image must be of type CV_8UC3")
                return
            }
        } catch(e: Exception) {
            scaledImg.returnMat()
            logger.error("Error while scaling streamed image", e)
            return
        }

        fun sendBytes() {
            synchronized(bytes) {
                scaledImg.get(0, 0, bytes)
                engine.sendBytes(byteTag, id, bytes)
                sleep(1)
            }
        }

        val latestToCurrentMaskMat = maskMatMap.getOrPut(id) { Mat() }
        val latestMat = latestMatMap.getOrPut(id) { Mat() }

        val changeRateTimer = changeRateTimers.getOrPut(id) { ElapsedTime() }
        val diffSlowDownTimer = diffSlowDownTimers.getOrPut(id) { ElapsedTime() }
        val changeRateAvg = changeRateAvgs.getOrPut(id) { MovingStatistics(50) }

        changeRateAvg.add(changeRateTimer.seconds)

        val mean = changeRateAvg.mean

        val isFastChange = mean <= 0.5

        // Perform the diff check only if the change rate is slow enough
        try {
            // Skip diff check if changes are frequent
            if (isFastChange) {
                sendBytes()
            } else if (!latestMat.empty() && latestMat.size() == scaledImg.size()) {
                // Slow down diff check after 3 seconds of no changes
                if(changeRateTimer.seconds > 3) {
                    val slowDown = ((changeRateTimer.seconds - 3) * 0.1).coerceAtMost(0.5)
                    if(diffSlowDownTimer.seconds <= slowDown) {
                        return
                    } else {
                        diffSlowDownTimer.reset()
                    }
                }

                Core.absdiff(latestMat, scaledImg, latestToCurrentMaskMat)
                Imgproc.cvtColor(latestToCurrentMaskMat, latestToCurrentMaskMat, Imgproc.COLOR_RGB2GRAY)

                val diffPixels = Core.countNonZero(latestToCurrentMaskMat)

                // If there are significant differences or change rate indicates slower changes, send bytes
                if (diffPixels > 0) {
                    sendBytes()
                    changeRateTimer.reset()
                }
            } else {
                sendBytes() // Send if there is no latest mat (first frame or no latest)
            }
        } finally {
            scaledImg.copyTo(latestMat) // Update the latest mat
            scaledImg.returnMat() // Return the scaled image mat
        }
    }

    fun stop() {
        logger.info("Stopping $tag EOCVSimEngineImageStreamer MatPoster")
        poster.stop()
    }

    private data class MatData(
        val id: Int,
        val cvtCode: Int?
    )
}