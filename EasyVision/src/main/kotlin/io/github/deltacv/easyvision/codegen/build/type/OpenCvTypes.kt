package io.github.deltacv.easyvision.codegen.build.type

import io.github.deltacv.easyvision.codegen.build.ConValue
import io.github.deltacv.easyvision.codegen.build.Type

object OpenCvTypes {

    val OpenCvPipeline = Type("OpenCvPipeline", "org.openftc.easyopencv")

    object Imgproc : Type("Imgproc", "org.opencv.imgproc") {

        val RETR_LIST = ConValue(StandardTypes.cint, "Imgproc.RETR_LIST")
        val CHAIN_APPROX_SIMPLE = ConValue(StandardTypes.cint, "Imgproc.CHAIN_APPROX_SIMPLE")

    }

    val Core = Type("Core", "org.opencv.core")

    val Mat = Type("Mat", "org.opencv.core")
    val MatOfInt = Type("MatOfInt", "org.opencv.core")
    val MatOfPoint = Type("MatOfPoint", "org.opencv.core")
    val MatOfPoint2f = Type("MatOfPoint2f", "org.opencv.core")

    val Size = Type("Size", "org.opencv.core")
    val Scalar = Type("Scalar", "org.opencv.core")
    val Rect = Type("Rect", "org.opencv.core")
    val Point = Type("Point", "org.opencv.core")

}