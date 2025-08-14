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
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.Resolvable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
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
    name = "nod_cvtcolor",
    category = Category.IMAGE_PROC,
    description = "des_cvtcolor"
)
class CvtColorNode : DrawNode<CvtColorNode.Session>() {

    val input  = MatAttribute(INPUT, "$[att_input]")
    val output = MatAttribute(OUTPUT, "$[att_output]").enablePrevizButton()

    val convertTo = EnumAttribute(INPUT, ColorSpace.values(), "$[att_convertto]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + convertTo.rebuildOnChange()

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val inputMat = input.value(current)
                inputMat.requireNonBinary(input)

                val targetColor = convertTo.value(current).value
                val matColor = inputMat.color

                val matColorResolved = matColor.resolve()

                if(matColorResolved == null || matColorResolved != targetColor) {
                    val mat = uniqueVariable("${targetColor.name.lowercase()}Mat", Mat.new())

                    group {
                        // create mat instance variable
                        private(mat)
                    }

                    current.scope { // add a cvtColor step in processFrame
                        writeNameComment()

                        deferredBlock(Resolvable.DependentPlaceholder(matColor) {
                            {
                                if(it != targetColor) {
                                    Imgproc("cvtColor", inputMat.value.v, mat, cvtColorValue(it, targetColor))
                                } else {
                                    // copyTo
                                    Imgproc("copyTo", inputMat.value.v, mat)
                                }
                            }
                        })

                        output.streamIfEnabled(mat, targetColor.resolved())
                    }

                    session.outputMatValue = GenValue.Mat(mat.resolved(), targetColor.resolved()) // store data in the current session
                } else {
                    // we don't need to do any processing if the mat is
                    // already of the color the user specified to convert to
                    session.outputMatValue = inputMat
                }

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val inputMat = input.value(current)
                inputMat.requireNonBinary(input)

                val targetColor = convertTo.value(current).value
                val matColor = inputMat.color
                val matColorResolved = matColor.resolve()

                current.scope {
                    writeNameComment()

                    if (matColorResolved == null || matColorResolved != targetColor) {
                        val value = Resolvable.DependentPlaceholder(matColor) {
                            if(it != targetColor) {
                                cv2.callValue("cvtColor", CPythonLanguage.NoType, inputMat.value.v, cvtColorValue(it, targetColor))
                            } else {
                                inputMat.value.v.callValue("copy", CPythonLanguage.NoType)
                            }
                        }.v

                        val mat = uniqueVariable("${inputMat.value.value}_${targetColor.name.lowercase()}", value)

                        local(mat)

                        session.outputMatValue = GenValue.Mat(mat.resolved(), targetColor.resolved()) // store data in the current session
                    } else {
                        // we don't need to do any processing if the mat is
                        // already of the color the user specified to convert to
                        session.outputMatValue = inputMat
                    }
                }
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