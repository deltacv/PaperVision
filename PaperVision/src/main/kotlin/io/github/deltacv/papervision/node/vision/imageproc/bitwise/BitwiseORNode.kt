/*
 * PaperVision
 * Copyright (C) 2025 Sebastian Erives, deltacv

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

package io.github.deltacv.papervision.node.vision.imageproc.bitwise

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_bitwiseor",
    category = Category.IMAGE_PROC,
    description = "des_bitwiseor"
)
class BitwiseORNode : DrawNode<BitwiseORNode.Session>() {

    val first = MatAttribute(INPUT, "$[att_first]")
    val second = MatAttribute(INPUT, "$[att_second]")

    val output = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + first.rebuildOnChange()
        + second.rebuildOnChange()

        + output.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val firstValue = first.value(current)
            val secondValue = second.value(current)

            if(firstValue.isBinary != secondValue.isBinary) {
                raise("err_bitwiseor_binary")
            }

            current {
                val outputMat = uniqueVariable("bitwiseORMat", Mat.new())

                group {
                    public(outputMat)
                }

                current.scope {
                    outputMat("release")
                    JvmOpenCvTypes.Core("bitwise_or", firstValue.value, secondValue.value, outputMat)

                    output.streamIfEnabled(outputMat, secondValue.color)
                }

                session.outputMatValue = GenValue.Mat(outputMat, secondValue.color, firstValue.isBinary && secondValue.isBinary)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val firstValue = first.value(current)
            val secondValue = second.value(current)

            if(firstValue.isBinary != secondValue.isBinary) {
                raise("err_bitwiseor_binary")
            }

            current {
                val value = CPythonOpenCvTypes.cv2.callValue("bitwise_or", CPythonLanguage.NoType, firstValue.value, secondValue.value)
                val variable = uniqueVariable("bitwiseORMat", value)

                current.scope {
                    local(variable)
                }

                session.outputMatValue = GenValue.Mat(variable, secondValue.color, firstValue.isBinary && secondValue.isBinary)
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return current.sessionOf(this)!!.outputMatValue
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}