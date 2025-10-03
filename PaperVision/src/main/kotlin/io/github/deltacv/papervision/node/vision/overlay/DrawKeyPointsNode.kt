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

package io.github.deltacv.papervision.node.vision.overlay

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.KeyPointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
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
    name = "nod_drawkeypoints",
    category = Category.OVERLAY,
    description = "des_drawkeypoints"
)
open class DrawKeyPointsNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false) : DrawNode<DrawKeyPointsNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val keypoints = KeyPointsAttribute(INPUT, "$[att_keypoints]")

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + lineColor

        + keypoints.rebuildOnChange()

        if (!isDrawOnInput) {
            + outputMat.enablePrevizButton().rebuildOnChange()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val color = lineColor.genValue(current)

                val input = inputMat.genValue(current)
                val keypointsValue = keypoints.genValue(current)
                val output = uniqueVariable("${input.value.value}KeyPoints", Mat.new())

                var drawMat = if (!isDrawOnInput) {
                    output
                } else {
                    input.value.v
                }

                val colorScalar = uniqueVariable(
                    "keypointColor",
                    JvmOpenCvTypes.Scalar.new(
                        color.a.value.v,
                        color.b.value.v,
                        color.c.value.v,
                        color.d.value.v
                    )
                )

                group {
                    if (!isDrawOnInput) {
                        private(output)
                    }
                    public(colorScalar, lineColor.label())
                }

                current.scope {
                    nameComment()

                    JvmOpenCvTypes.Features2d("drawKeypoints", input.value.v, keypointsValue.value.v, drawMat, colorScalar)

                    if (!isDrawOnInput) {
                        outputMat.streamIfEnabled(output, input.color)
                    }
                }

                session.outputMat = GenValue.Mat(drawMat.resolved(), input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val color = lineColor.genValue(current)

                val input = inputMat.genValue(current)
                val keypointsValue = keypoints.genValue(current)

                current.scope {
                    nameComment()

                    val output = uniqueVariable("${input.value.value}_keypoints",
                        cv2.callValue("drawKeypoints",
                            CPythonLanguage.NoType,
                            input.value.v,
                            keypointsValue.value.v,
                            CPythonOpenCvTypes.np.callValue("array",
                                CPythonLanguage.NoType, CPythonLanguage.newArrayOf(CPythonLanguage.NoType, 0.v)
                            ),
                            CPythonLanguage.tuple(color.a.value.v, color.b.value.v, color.c.value.v)
                        )
                    )

                    local(output)
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if (attrib == outputMat) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMat }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@PaperNode(
    name = "nod_drawrects_onimage",
    category = Category.OVERLAY,
    description = "des_drawrects_onimage",
    showInList = false // executive decision
)
class DrawKeyPointsOnImageNode : DrawKeyPointsNode(true)