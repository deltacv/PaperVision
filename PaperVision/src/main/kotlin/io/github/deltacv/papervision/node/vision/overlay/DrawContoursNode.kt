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
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
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

        + lineParams

        + contours.rebuildOnChange()

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

                val lineParams = lineParams.genValue(current).ensureRuntimeLineJvm(current)

                val input = inputMat.genValue(current)
                input.requireNonBinary(inputMat)

                val contoursList = contours.genValue(current)

                val output = uniqueVariable("${input.value.v}Contours", Mat.new())
                var drawMat = input.value.v

                group {
                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    nameComment()

                    if(!isDrawOnInput) {
                        drawMat = output
                        input.value.v("copyTo", drawMat)
                    }

                    if(contoursList is GenValue.GList.RuntimeListOf<*>) {
                        Imgproc("drawContours", drawMat, contoursList.value.v, (-1).v,
                            lineParams.colorScalarValue.v,
                            lineParams.thicknessValue.v
                        )
                    } else {
                        separate()

                        val list = DeclarableVariable("contoursList", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())
                        local(list)

                        for (contour in (contoursList as GenValue.GList.ListOf<*>).elements) {
                            if (contour is GenValue.GPoints.RuntimePoints) {
                                ifCondition(contour.value.v notEqualsTo language.nullValue) {
                                    list("add", contour.value.v)
                                }
                            } else {
                                raise("Points are not supported")
                            }
                        }

                        separate()

                        Imgproc("drawContours", drawMat, list, (-1).v,
                            lineParams.colorScalarValue.v,
                            lineParams.thicknessValue.v
                        )
                    }

                    if(!isDrawOnInput) {
                        outputMat.streamIfEnabled(drawMat, input.color)
                    }
                }

                session.outputMat = GenValue.Mat(drawMat.resolved(), input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val input = inputMat.genValue(current)
                input.requireNonBinary(inputMat)

                val contoursList = contours.genValue(current)

                val lineParams = lineParams.genValue(current)
                if(lineParams !is GenValue.LineParameters.Line) {
                    raise("Given line parameters is not a static type")
                }

                current.scope {
                    nameComment()

                    val color = lineParams.color
                    val thickness = lineParams.thickness.value

                    val colorScalar = CPythonLanguage.tuple(color.a.value.v, color.b.value.v, color.c.value.v, color.d.value.v)

                    val target = if(isDrawOnInput) {
                        input.value.v
                    } else {
                        val output = uniqueVariable(
                            "${input.value}_contours", input.value.v.callValue("copy", CPythonLanguage.NoType)
                        )
                        local(output)

                        output
                    }

                    if(contoursList is GenValue.GList.RuntimeListOf<*>) {
                        cv2("drawContours", target, contoursList.value.v, (-1).v, colorScalar, thickness.v)
                    } else {
                        separate()

                        val list = uniqueVariable("contoursList", CPythonLanguage.NoType.newArray())
                        local(list)

                        for(contour in (contoursList as GenValue.GList.ListOf<*>).elements) {
                            if(contour is GenValue.GPoints.RuntimePoints) {
                                ifCondition(contour.value.v notEqualsTo CPythonLanguage.nullValue) {
                                    list("append", contour.value.v)
                                }
                            } else {
                                raise("Invalid contour type")
                            }
                        }

                        separate()

                        cv2("drawContours", target, list, (-1).v, colorScalar, thickness.v)
                    }

                    session.outputMat = GenValue.Mat(target.resolved(), input.color, input.isBinary)
                }
            }

            session
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

@PaperNode(
    name = "nod_drawcontours_onimage",
    category = Category.OVERLAY,
    description = "des_drawcontours_onimage",
    showInList = false // executive decision
)
class DrawContoursOnImageNode : DrawContoursNode(true)
