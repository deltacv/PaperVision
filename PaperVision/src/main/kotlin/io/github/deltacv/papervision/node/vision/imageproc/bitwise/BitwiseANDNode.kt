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
import io.github.deltacv.papervision.codegen.Resolvable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_bitwiseand",
    category = Category.IMAGE_PROC,
    description = "des_bitwiseand"
)
class BitwiseANDNode : DrawNode<BitwiseANDNode.Session>() {

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

            Resolvable.DoubleDependentPlaceholder(firstValue.isBinary.value, secondValue.isBinary.value) { first, second ->
                first == second
            }.letOrDefer {
                raiseAssert(it, "err_bitwiseor_binary")
            }

            current {
                val outputMat = uniqueVariable("bitwiseANDMat", Mat.new())

                group {
                    public(outputMat)
                }

                current.scope {
                    nameComment()

                    outputMat("release")
                    JvmOpenCvTypes.Core("bitwise_and", firstValue.value.v, secondValue.value.v, outputMat)
                    output.streamIfEnabled(outputMat, secondValue.color)
                }

                val isBinaryPlaceholder = Resolvable.DoubleDependentPlaceholder(firstValue.isBinary.value, secondValue.isBinary.value) { first, second ->
                    first && second
                }
                val isBinary = GenValue.Boolean(isBinaryPlaceholder)

                session.outputMatValue = GenValue.Mat(outputMat.resolved(), secondValue.color, isBinary)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val firstValue = first.value(current)
            val secondValue = second.value(current)

            Resolvable.DoubleDependentPlaceholder(firstValue.isBinary.value, secondValue.isBinary.value) { first, second ->
                first == second
            }.letOrDefer {
                raiseAssert(it, "err_bitwiseor_binary")
            }

            current {
                val value = CPythonOpenCvTypes.cv2.callValue("bitwise_and", CPythonLanguage.NoType, firstValue.value.v, secondValue.value.v)
                val variable = uniqueVariable("bitwiseANDMat", value)

                current.scope {
                    nameComment()
                    local(variable)
                }

                val isBinaryPlaceholder = Resolvable.DoubleDependentPlaceholder(firstValue.isBinary.value, secondValue.isBinary.value) { first, second ->
                    first && second
                }
                val isBinary = GenValue.Boolean(isBinaryPlaceholder)

                session.outputMatValue = GenValue.Mat(variable.resolved(), secondValue.color, isBinary)
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
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