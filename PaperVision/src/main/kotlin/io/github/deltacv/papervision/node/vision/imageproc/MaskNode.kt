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
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Core
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_binarymask",
    category = Category.IMAGE_PROC,
    description = "des_binarymask"
)
class MaskNode : DrawNode<MaskNode.Session>(){

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val maskMat  = MatAttribute(INPUT, "$[att_binarymask]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]").enablePrevizButton()

    override fun onEnable() {
        + inputMat.rebuildOnChange()
        + maskMat.rebuildOnChange()

        + outputMat.rebuildOnChange()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputMat.value(current)
                input.requireNonBinary(inputMat)

                val mask = maskMat.value(current)
                mask.requireBinary(maskMat)

                val output = uniqueVariable("${input.value.value!!}Mask", Mat.new())

                group {
                    private(output)
                }

                current.scope {
                    output("release")
                    Core("bitwise_and", input.value, input.value, output, mask.value)
                    outputMat.streamIfEnabled(output, input.color)
                }

                session.outputMat = GenValue.Mat(output, input.color)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val input = inputMat.value(current)
                input.requireNonBinary(inputMat)

                val mask = maskMat.value(current)
                mask.requireBinary(maskMat)

                current.scope {
                    val output = uniqueVariable("${input.value.value!!}_mask",
                        cv2.callValue("bitwise_and", CPythonLanguage.NoType, input.value, input.value, CPythonLanguage.namedArgument("mask", mask.value))
                    )
                    local(output)

                    session.outputMat = GenValue.Mat(output, input.color)
                }

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return lastGenSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}