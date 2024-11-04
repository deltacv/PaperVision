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
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_drawcontours",
    category = Category.OVERLAY,
    description = "des_drawcontours"
)
open class DrawContoursNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false)
    : DrawNode<DrawContoursNode.Session>()  {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val contours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val lineParams = LineParametersAttribute(INPUT, "$[att_params]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()
        + contours.rebuildOnChange()

        + lineParams

        if(!isDrawOnInput) {
            + outputMat.enablePrevizButton().rebuildOnChange()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val lineParams = (lineParams.value(current) as GenValue.LineParameters).ensureRuntimeLine(current)

                val input = inputMat.value(current)
                val contoursList = contours.value(current)

                val output = uniqueVariable("${input.value.value!!}Contours", Mat.new())

                if(contoursList !is GenValue.GList.RuntimeListOf<*>) {
                    contours.raise("Given list is not a runtime type (TODO)") // TODO: Handle non-runtime lists
                }

                var drawMat = input.value

                group {
                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    if(!isDrawOnInput) {
                        drawMat = output
                        input.value("copyTo", drawMat)
                    }

                    Imgproc("drawContours", drawMat, contoursList.value, (-1).v,
                        lineParams.colorScalarValue,
                        lineParams.thicknessValue
                    )

                    if(!isDrawOnInput) {
                        outputMat.streamIfEnabled(drawMat, input.color)
                    }
                }

                session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val input = inputMat.value(current)
                val contoursList = contours.value(current)

                val lineParams = lineParams.value(current)
                if(lineParams !is GenValue.LineParameters.Line) {
                    raise("Given line parameters is not a static type")
                }

                current.scope {
                    if(contoursList !is GenValue.GList.RuntimeListOf<*>) {
                        contours.raise("Given list is not a runtime type (TODO)") // TODO: Handle non-runtime lists
                    }

                    val color = lineParams.color
                    val thickness = lineParams.thickness.value

                    val colorScalar = CPythonLanguage.tuple(color.a.v, color.b.v, color.c.v, color.d.v)

                    val target = if(isDrawOnInput) {
                        input.value
                    } else {
                        val output = uniqueVariable(
                            "${input.value.value!!}_contours", input.value.callValue("copy", CPythonLanguage.NoType)
                        )
                        local(output)

                        output
                    }

                    cv2("drawContours", target, contoursList.value, (-1).v, colorScalar, thickness.v)

                    session.outputMat = GenValue.Mat(target, input.color, input.isBinary)
                }
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return current.sessionOf(this)!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@PaperNode(
    name = "nod_drawcontours_onimage",
    category = Category.OVERLAY,
    description = "des_drawcontours_onimage",
    showInList = false // executive decision
)
class DrawContoursOnImageNode : DrawContoursNode(true)