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
import io.github.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.np
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_drawrotated_rects",
    category = Category.OVERLAY,
    description = "des_drawrotated_rects"
)
open class DrawRotatedRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false) : DrawNode<DrawRotatedRectanglesNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val rectangles = ListAttribute(INPUT, RotatedRectAttribute, "$[att_rects]")

    val lineParams = LineParametersAttribute(INPUT, "$[att_params]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + lineParams

        + rectangles.rebuildOnChange()

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

                val lineParams = lineParams.genValue(current).ensureRuntimeLineJava(current)

                val input = inputMat.genValue(current)
                val rectanglesList = rectangles.genValue(current)

                val output = uniqueVariable("${input.value.v}RotRects", Mat.new())

                var drawMat = input.value.v

                group {
                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    nameComment()

                    if (!isDrawOnInput) {
                        drawMat = output
                        input.value.v("copyTo", drawMat)
                    }

                    fun ScopeContext.drawRuntimeRect(rectValue: Value) {
                        ifCondition(rectValue notEqualsTo language.nullValue) {
                            val rectPoints = uniqueVariable("rectPoints", JvmOpenCvTypes.Point.newArray(4.v))
                            local(rectPoints)
                            rectValue("points", rectPoints)

                            val matOfPoint = uniqueVariable("matOfPoint", JvmOpenCvTypes.MatOfPoint.new(rectPoints))
                            local(matOfPoint)

                            separate()

                            // Draw the rectangle using the points
                            ifCondition((drawMat notEqualsTo language.nullValue) and not(drawMat.callValue("empty", BooleanType).condition())) {
                                Imgproc(
                                    "polylines", drawMat,
                                    JavaTypes.Collections.callValue(
                                        "singletonList",
                                        JavaTypes.List(JvmOpenCvTypes.MatOfPoint),
                                        matOfPoint
                                    ), // list of points forming the rotated rectangle
                                    trueValue, // closed polygon
                                    lineParams.colorScalarValue.v,
                                    lineParams.thicknessValue.v
                                )
                            }
                        }
                    }

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rotated.RotatedRect) {
                                TODO("")
                            } else if (rectangle is GenValue.GRect.Rotated.RuntimeRotatedRect) {
                                drawRuntimeRect(rectangle.value.v)
                            }
                        }
                    } else {
                        foreach(variable(JvmOpenCvTypes.RotatedRect, "rect"), rectanglesList.value.v) {
                            drawRuntimeRect(it)
                        }
                    }

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
                val input = inputMat.genValue(current)
                val rectanglesList = rectangles.genValue(current)

                val lineParams = (lineParams.genValue(current))

                if(lineParams !is GenValue.LineParameters.Line) {
                    raise("Line parameters must not be runtime")
                }

                current.scope {
                    nameComment()

                    val target = if (isDrawOnInput) {
                        input.value.v
                    } else {
                        val output = uniqueVariable(
                            "${input.value.v}_rot_rects",
                            input.value.v.callValue("copy", CPythonLanguage.NoType)
                        )
                        local(output)
                        output
                    }

                    val color = lineParams.color
                    val thickness = lineParams.thickness.value

                    val colorScalar = CPythonLanguage.tuple(color.a.value.v, color.b.value.v, color.c.value.v, color.d.value.v)

                    // TODO: Implement rotated rect drawing in python

                    fun ScopeContext.runtimeRect(rectValue: Value) {
                        ifCondition(rectValue notEqualsTo language.nullValue) {
                            val box = uniqueVariable("box", cv2.callValue("boxPoints", CPythonLanguage.NoType, rectValue))
                            local(box)
                            box set np.callValue("int0", CPythonLanguage.NoType, box)
                            cv2("drawContours", target, box, (0).v, colorScalar, thickness.v)
                        }
                    }

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rotated.RotatedRect) {
                                raise("RotatedRects are not supported")
                            } else if (rectangle is GenValue.GRect.RuntimeRect) {
                                runtimeRect(rectangle.value.v)
                            }
                        }
                    } else {
                        foreach(variable(CPythonLanguage.NoType, "rect"), rectanglesList.value.v) { rect ->
                            runtimeRect(rect)
                        }
                    }

                    session.outputMat = GenValue.Mat(target.resolved(), input.color, input.isBinary)
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