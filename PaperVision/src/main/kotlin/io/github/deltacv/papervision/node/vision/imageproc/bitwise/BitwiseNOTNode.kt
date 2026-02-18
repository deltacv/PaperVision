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

package io.github.deltacv.papervision.node.vision.imageproc.bitwise

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.language.cpython.CPythonOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_bitwisenot",
    category = NodeCategory.IMAGE_PROC,
    description = "des_bitwisenot"
)
class BitwiseNOTNode : DrawNode<BitwiseNOTNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val output = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + output.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val firstValue = input.genValue(current)

            current {
                val outputMat = uniqueVariable("bitwiseNOTMat", Mat.new())

                group {
                    public(outputMat)
                }

                current.scope {
                    nameComment()

                    outputMat("release")
                    JvmOpenCv.Core("bitwise_not", firstValue.value.v, outputMat)

                    output.streamIfEnabled(outputMat, firstValue.color)
                }

                session.outputMatValue = GenValue.Mat(outputMat.resolved(), firstValue.color, firstValue.isBinary)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val firstValue = input.genValue(current)

            current {
                val value = CPythonOpenCv.cv2.callValue("bitwise_not", CPythonLanguage.NoType, firstValue.value.v)
                val variable = uniqueVariable("bitwiseNOTMat", value)

                current.scope {
                    nameComment()
                    local(variable)
                }

                session.outputMatValue = GenValue.Mat(variable.resolved(), firstValue.color, firstValue.isBinary)
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == output) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMatValue }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}
