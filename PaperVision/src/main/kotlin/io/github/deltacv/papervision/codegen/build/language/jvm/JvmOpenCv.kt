/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen.build.language.jvm

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.GenValue.Vec2.Actual
import io.github.deltacv.papervision.codegen.GenValue.Vec2.Runtime
import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Parameter
import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.language.StandardTypes
import io.github.deltacv.papervision.codegen.dsl.LanguageContext
import io.github.deltacv.papervision.codegen.resolve.Resolvable
import io.github.deltacv.papervision.codegen.resolve.resolved

object JvmOpenCv {

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

    fun Scalar(genValue: GenValue.Scalar, langHolder: CodeGen.LanguageHolder) = langHolder.language {
        when (genValue) {
            is GenValue.Scalar.Inst -> ConValue(Scalar, genValue.value.v.value)
            is GenValue.Scalar.Components -> Scalar.new(genValue.a.v, genValue.b.v, genValue.c.v, genValue.d.v)
        }
    }

    val Rect = Type("Rect", "org.opencv.core")

    fun toRectInst(rect: GenValue.Rect, langHolder: CodeGen.LanguageHolder) = when(rect) {
        is GenValue.Rect.Components -> langHolder.language {
            GenValue.Rect.Inst(Rect.new(rect.x.toInt(langHolder).v, rect.y.toInt(langHolder).v, rect.w.toInt(langHolder).v, rect.h.toInt(langHolder).v).resolved())
        }

        is GenValue.Rect.Inst -> rect
    }

    val RotatedRect = Type("RotatedRect", "org.opencv.core")

    val Point = Type("Point", "org.opencv.core")
    val KeyPoint = Type("KeyPoint", "org.opencv.core")

    object SimpleBlobDetector : Type("SimpleBlobDetector", "org.opencv.features2d") {
        val Params = Type("SimpleBlobDetector_Params", "org.opencv.features2d")
    }

    fun toRuntimeLineParameters(line: GenValue.LineParameters, current: CodeGen.Current): GenValue.LineParameters.Runtime {
        return current {
            when (line) {
                is GenValue.LineParameters.Actual -> {
                    val color = uniqueVariable(
                        "lineColor", Scalar.new(
                            line.color.a.v,
                            line.color.b.v,
                            line.color.c.v,
                            line.color.d.v
                        )
                    )

                    val thickness = uniqueVariable("lineThickness", line.thickness.value.v)

                    group {
                        public(color)
                        public(thickness)
                    }

                    GenValue.LineParameters.Runtime(GenValue.Scalar.Inst(Resolvable.Now(color)), GenValue.Int.Runtime(Resolvable.Now(thickness)))
                }

                is GenValue.LineParameters.Runtime -> line
            }
        }
    }

    fun toRuntimeVec2(vec2: GenValue.Vec2, langHolder: CodeGen.LanguageHolder): GenValue.Vec2.Runtime {
        return langHolder.language {
            when (vec2) {
                is Actual -> Runtime(vec2.x.toRuntime(langHolder), vec2.y.toRuntime(langHolder))
                is Runtime -> vec2
            }
        }
    }

    fun getCircleType(current: CodeGen.Current): Type {
        val circleType = Type("Circle", "Circle")
        current {
            if (!codeGen.hasFlag("circleTypeEnabled")) {
                codeGen.classEndScope {
                    clazz(Visibility.PACKAGE_PRIVATE, "Circle", isStatic = true) {
                        val centerVariable = DeclarableVariable(Point, "center")
                        val radiusVariable = DeclarableVariable(DoubleType, "radius")

                        group {
                            instanceVariable(
                                Visibility.PACKAGE_PRIVATE,
                                centerVariable,
                                isFinal = true
                            )

                            instanceVariable(
                                Visibility.PACKAGE_PRIVATE,
                                radiusVariable,
                                isFinal = true
                            )
                        }

                        separate()

                        val centerParameter = Parameter(Point, "center")
                        val radiusParameter = Parameter(FloatType, "radius")

                        constructor(Visibility.PACKAGE_PRIVATE, circleType, centerParameter, radiusParameter) {
                            centerVariable instanceSet centerParameter
                            radiusVariable instanceSet radiusParameter
                        }
                    }
                }

                codeGen.addFlag("circleTypeEnabled")
            }
        }

        return circleType
    }
}
