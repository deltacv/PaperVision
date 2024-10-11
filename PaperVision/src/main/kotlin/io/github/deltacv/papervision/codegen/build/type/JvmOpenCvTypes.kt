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
        val CHAIN_APPROX_SIMPLE = ConValue(StandardTypes.cint, "Imgproc.CHAIN_APPROX_SIMPLE").apply {
            additionalImports(this)
        }

        val MORPH_RECT = ConValue(StandardTypes.cint, "Imgproc.MORPH_RECT").apply {
            additionalImports(this)
        }
    }

    object CvType : Type("CvType", "org.opencv.core")

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