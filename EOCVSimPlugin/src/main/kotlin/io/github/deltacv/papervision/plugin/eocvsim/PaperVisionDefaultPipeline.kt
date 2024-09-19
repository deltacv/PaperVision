package io.github.deltacv.papervision.plugin.eocvsim

import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.OpenCvPipeline

@Disabled
class PaperVisionDefaultPipeline(
    val telemetry: Telemetry
) : OpenCvPipeline() {

    val logger by loggerForThis()

    lateinit var drawMat: Mat

    override fun init(mat: Mat) {
        drawMat = Mat(mat.size(), mat.type())
        drawMat.setTo(Scalar(0.0, 0.0, 0.0, 0.0))

        try {
            val bytes = PaperVisionDefaultPipeline::class.java.getResourceAsStream("/ico/ico_ezv.png")!!.use {
                it.readBytes()
            }

            println(bytes.size)

            val logoBytes = MatOfByte(*bytes)
            val logoMat = Imgcodecs.imdecode(logoBytes, Imgcodecs.IMREAD_UNCHANGED)
            logoBytes.release()

            Imgproc.cvtColor(logoMat, logoMat, Imgproc.COLOR_BGR2RGBA)

            Imgproc.resize(logoMat, logoMat, Size(logoMat.size().width / 2, logoMat.size().width / 2))

            // Draw the logo centered
            val x = (drawMat.width() - logoMat.width()) / 2
            val y = (drawMat.height() - logoMat.height()) / 2

            val roi = drawMat.submat(y, y + logoMat.height(), x, x + logoMat.width())
            logoMat.copyTo(roi)

            logoMat.release()
        } catch(e: Exception) {
            logger.warn("Failed to load logo", e)
        }
    }

    override fun processFrame(input: Mat): Mat {
        telemetry.addLine("Making computer vision accessible to everyone")
        telemetry.update()
        return drawMat
    }

}