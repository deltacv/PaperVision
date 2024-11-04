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

package io.github.deltacv.papervision.plugin.eocvsim

import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipeline
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
) : StreamableOpenCvPipeline() {

    val logger by loggerForThis()

    lateinit var drawMat: Mat

    override fun init(mat: Mat) {
        drawMat = Mat(mat.size(), mat.type())
        drawMat.setTo(Scalar(0.0, 0.0, 0.0, 0.0))

        try {
            val bytes = PaperVisionDefaultPipeline::class.java.getResourceAsStream("/ico/ico_ezv.png")!!.use {
                it.readBytes()
            }

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

        streamFrame(0, input, null)

        return drawMat
    }

}