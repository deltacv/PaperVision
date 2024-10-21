package io.github.deltacv.papervision.plugin.ipc.eocvsim

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.papervision.engine.PaperVisionEngine
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.vision.external.util.extension.aspectRatio
import io.github.deltacv.vision.external.util.extension.clipTo
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler

class EOCVSimEngineImageStreamer(
    val engine: PaperVisionEngine,
    tag: String,
    resolution: Size
) : ImageStreamer {

    val matRecycler = MatRecycler(3)
    private var bytes = ByteArray(resolution.width.toInt() * resolution.height.toInt() * 3)

    private val latestMatMap = mutableMapOf<Int, Mat>()
    private val maskMatMap = mutableMapOf<Int, Mat>()

    var resolution = resolution
        set(value) {
            field = value
            bytes = ByteArray(value.width.toInt() * value.height.toInt() * 3)
        }

    val byteTag = ByteMessageTag.fromString(tag)

    val logger by loggerForThis()

    override fun sendFrame(
        id: Int,
        image: Mat,
        cvtCode: Int?
    ) {
        if(image.empty()) return

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
            }
        }

        val latestToCurrentMaskMat = maskMatMap.getOrPut(id) { Mat() }
        val latestMat = latestMatMap.getOrPut(id) { Mat() }

        // create a mask mat to check if the image has changed
        // if it hasn't, we don't need to send it
        try {
            if (!latestMat.empty() && latestMat.size() == scaledImg.size()) {
                latestToCurrentMaskMat.release()

                Core.bitwise_xor(latestMat, scaledImg, latestToCurrentMaskMat)
                Core.extractChannel(latestToCurrentMaskMat, latestToCurrentMaskMat, 0)

                if (Core.countNonZero(latestToCurrentMaskMat) == 0) {
                    return
                } else {
                    sendBytes()
                }
            } else {
                sendBytes()
            }
        } finally {
            scaledImg.copyTo(latestMat) // update latest mat
            scaledImg.returnMat()
        }
    }

}