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

package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Size
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage.tuple
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.imageproc.BlurAlgorithm.*

enum class BlurAlgorithm { Gaussian, Box, Median, Bilateral }

@PaperNode(
    name = "nod_blur",
    category = Category.IMAGE_PROC,
    description = "des_blur"
)
class BlurNode : DrawNode<BlurNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    val blurAlgo = EnumAttribute(INPUT, values(), "$[att_bluralgo]")
    val blurValue = IntAttribute(INPUT, "$[att_value]")

    val output = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + blurAlgo
        + blurValue
        + output.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val inputMat = input.genValue(current)
                val algo = blurAlgo.genValue(current).value
                val blurVal = blurValue.genValue(current)

                val blurValVariable = uniqueVariable("blurValue", int(blurVal.value.v))
                val outputMat = uniqueVariable("blur${algo.name}Mat", Mat.new())

                group {
                    public(blurValVariable, blurValue.label())
                    private(outputMat)
                }

                current.scope {
                    nameComment()

                    when(algo) {
                        Gaussian -> {
                            val kernelSize = 6.v * blurValVariable + 1.v
                            val sizeBlurVal = Size.new(kernelSize, kernelSize)

                            Imgproc("GaussianBlur", inputMat.value.v, outputMat, sizeBlurVal, blurValVariable)
                        }
                        Box -> {
                            val kernelSize = 2.v * blurValVariable + 1.v
                            val sizeBlurVal = Size.new(kernelSize, kernelSize)

                            Imgproc("blur", inputMat.value.v, outputMat, sizeBlurVal)
                        }
                        Median -> {
                            val kernelSize = 2.v * blurValVariable + 1.v
                            Imgproc("medianBlur", inputMat.value.v, outputMat, kernelSize)
                        }
                        Bilateral -> {
                            Imgproc("bilateralFilter", inputMat.value.v, outputMat, (-1).v, blurValVariable, blurValVariable)
                        }
                    }


                    output.streamIfEnabled(outputMat, inputMat.color)
                }

                session.outputMatValue = GenValue.Mat(outputMat.resolved(), inputMat.color)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val inputMat = input.genValue(current)
                val algo = blurAlgo.genValue(current).value
                val blurVal = blurValue.genValue(current)

                current.scope {
                    nameComment()

                    val value = when (algo) {
                        Gaussian -> {
                            val kernelSize = uniqueVariable("kernel", 6.v * int(blurVal.value.v) + 1.v)
                            local(kernelSize)
                            val sizeBlurVal = tuple(kernelSize, kernelSize)

                            cv2.callValue("GaussianBlur", CPythonLanguage.NoType, inputMat.value.v, sizeBlurVal, int(blurVal.value.v))
                        }

                        Box -> {
                            val kernelSize = uniqueVariable("kernel", 2.v * int(blurVal.value.v) + 1.v)
                            local(kernelSize)

                            cv2.callValue("blur", CPythonLanguage.NoType, inputMat.value.v, tuple(kernelSize, kernelSize))
                        }

                        Median -> {
                            val kernelSize = 2.v * int(blurVal.value.v) + 1.v
                            cv2.callValue("medianBlur", CPythonLanguage.NoType, inputMat.value.v, kernelSize)
                        }

                        Bilateral -> {
                            cv2.callValue(
                                "bilateralFilter",
                                CPythonLanguage.NoType,
                                inputMat.value.v,
                                (-1).v,
                                int(blurVal.value.v),
                                int(blurVal.value.v)
                            )
                        }
                    }

                    val variable = uniqueVariable("blur_${algo.name.lowercase()}", value)
                    local(variable)

                    session.outputMatValue = GenValue.Mat(variable.resolved(), inputMat.color)
                }

            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMatValue }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}
