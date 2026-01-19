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
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_cannyedge",
    category = Category.IMAGE_PROC,
    description = "des_cannyedge"
)
class CannyEdgeNode : DrawNode<CannyEdgeNode.Session>(){

    val inputMat = MatAttribute(INPUT, "$[att_input]")

    val firstThreshold = IntAttribute(INPUT, "$[att_lowerthreshold]")
    val secondThreshold = IntAttribute(INPUT, "$[att_upperthreshold]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + firstThreshold
        + secondThreshold

        + outputMat.rebuildOnChange().enablePrevizButton()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputMat.genValue(current)
                input.requireNonBinary(inputMat)

                input.color.letOrDefer {
                    if(it != ColorSpace.GRAY) {
                        inputMat.raise("err_grayscale_required")
                    }
                }

                val output = uniqueVariable("${input.value.v}Canny", Mat.new())

                val firstThresholdValue = firstThreshold.genValue(current).value.v
                val firstThresholdVariable = uniqueVariable("cannyFirstThreshold", int(firstThresholdValue))

                val secondThresholdValue = secondThreshold.genValue(current).value.v
                val secondThresholdVariable = uniqueVariable("cannySecondThreshold", int(firstThresholdValue))

                group {
                    private(output)

                    public(firstThresholdVariable, firstThreshold.label())
                    public(secondThresholdVariable, secondThreshold.label())
                }

                current.scope {
                    nameComment()

                    JvmOpenCvTypes.Imgproc("Canny", input.value.v, output, firstThresholdVariable, secondThresholdVariable)
                    outputMat.streamIfEnabled(output, input.color)
                }

                session.outputMat = GenValue.Mat(output.resolved(), input.color)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val input = inputMat.genValue(current)
                input.requireNonBinary(inputMat)

                input.color.letOrDefer {
                    if(it != ColorSpace.GRAY) {
                        inputMat.raise("err_grayscale_required")
                    }
                }

                current.scope {
                    nameComment()

                    val output = uniqueVariable("${input.value}_canny",
                        cv2.callValue("Canny", CPythonLanguage.NoType, input.value.v, firstThreshold.genValue(current).value.v, secondThreshold.genValue(current).value.v)
                    )

                    session.outputMat = GenValue.Mat(output.resolved(), input.color)
                }

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMat }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}