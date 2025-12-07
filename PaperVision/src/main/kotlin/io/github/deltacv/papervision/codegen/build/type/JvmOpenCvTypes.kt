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

package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Type

object JvmOpenCvTypes {

    val OpenCvPipeline = Type("OpenCvPipeline", "org.openftc.easyopencv")
    val StreamableOpenCvPipeline = Type("StreamableOpenCvPipeline", "io.github.deltacv.eocvsim.pipeline")

    object Imgproc : Type("Imgproc", "org.opencv.imgproc") {
        val RETR_LIST = ConValue(StandardTypes.cint, "Imgproc.RETR_LIST").apply {
            additionalImports(this)
        }

        val RETR_EXTERNAL = ConValue(StandardTypes.cint, "Imgproc.RETR_EXTERNAL").apply {
            additionalImports(this)
        }

        val CHAIN_APPROX_SIMPLE = ConValue(StandardTypes.cint, "Imgproc.CHAIN_APPROX_SIMPLE").apply {
            additionalImports(this)
        }

        val MORPH_RECT = ConValue(StandardTypes.cint, "Imgproc.MORPH_RECT").apply {
            additionalImports(this)
        }

        val HOUGH_GRADIENT = ConValue(StandardTypes.cint, "Imgproc.HOUGH_GRADIENT").apply {
            additionalImports(this)
        }
    }

    val Features2d = Type("Features2d", "org.opencv.features2d")

    object CvType : Type("CvType", "org.opencv.core")

    val Core = Type("Core", "org.opencv.core")

    val Mat = Type("Mat", "org.opencv.core")
    val MatOfInt = Type("MatOfInt", "org.opencv.core")
    val MatOfPoint = Type("MatOfPoint", "org.opencv.core")
    val MatOfPoint2f = Type("MatOfPoint2f", "org.opencv.core")
    val MatOfKeyPoint = Type("MatOfKeyPoint", "org.opencv.core")

    val Size = Type("Size", "org.opencv.core")
    val Scalar = Type("Scalar", "org.opencv.core")

    val Rect = Type("Rect", "org.opencv.core")
    val RotatedRect = Type("RotatedRect", "org.opencv.core")

    val Point = Type("Point", "org.opencv.core")
    val KeyPoint = Type("KeyPoint", "org.opencv.core")

    object SimpleBlobDetector : Type("SimpleBlobDetector", "org.opencv.features2d") {
        val Params = Type("SimpleBlobDetector_Params", "org.opencv.features2d")
    }

}