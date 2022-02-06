package io.github.deltacv.easyvision.codegen.build.type

import io.github.deltacv.easyvision.codegen.build.Type

object OpenCvTypes {

    val OpenCvPipeline = Type("OpenCvPipeline", "org.openftc.easyopencv")

    val Imgproc = Type("Imgproc", "org.opencv.imgproc")
    val Core = Type("Core", "org.opencv.core")

    val Mat = Type("Mat", "org.opencv.core")
    val MatOfPoint = Type("MatOfPoint", "org.opencv.core")

    val Scalar = Type("Scalar", "org.opencv.core")
    val Rect = Type("Rect", "org.opencv.core")

}