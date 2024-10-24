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
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
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
        + rectangles.rebuildOnChange()

        + lineParams

        if (!isDrawOnInput) {
            + outputMat.enablePrevizButton().rebuildOnChange()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val lineParams = (lineParams.value(current) as GenValue.LineParameters.Line).ensureRuntimeLine(current)

                val input = inputMat.value(current)
                val rectanglesList = rectangles.value(current)

                val output = uniqueVariable("${input.value.value!!}RotRects", Mat.new())

                var drawMat = input.value

                group {
                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    if (!isDrawOnInput) {
                        drawMat = output
                        input.value("copyTo", drawMat)
                    }

                    fun ScopeContext.drawRuntimeRect(rectValue: Value) {
                        val rectPoints = uniqueVariable("rectPoints", JvmOpenCvTypes.Point.newArray(4.v))
                        local(rectPoints)
                        rectValue("points", rectPoints)

                        val matOfPoint = uniqueVariable("matOfPoint", JvmOpenCvTypes.MatOfPoint.new(rectPoints))
                        local(matOfPoint)

                        separate()

                        // Draw the rectangle using the points
                        Imgproc(
                            "polylines", drawMat,
                            JavaTypes.Collections.callValue("singletonList", JavaTypes.List(JvmOpenCvTypes.MatOfPoint), matOfPoint), // list of points forming the rotated rectangle
                            trueValue, // closed polygon
                            lineParams.colorScalarValue,
                            lineParams.thicknessValue
                        )
                    }

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rect) {
                                TODO("")
                            } else if (rectangle is GenValue.GRect.Rotated.RuntimeRotatedRect) {
                                drawRuntimeRect(rectangle.value)
                            }
                        }
                    } else {
                        foreach(variable(JvmOpenCvTypes.RotatedRect, "rect"), rectanglesList.value) {
                            drawRuntimeRect(it)
                        }
                    }

                    if (!isDrawOnInput) {
                        outputMat.streamIfEnabled(output, input.color)
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
                val rectanglesList = rectangles.value(current)

                val lineParams = (lineParams.value(current))

                if(lineParams !is GenValue.LineParameters.Line) {
                    raise("Line parameters must not be runtime")
                }

                current.scope {
                    val target = if (isDrawOnInput) {
                        input.value
                    } else {
                        val output = uniqueVariable(
                            "${input.value.value}_rects",
                            input.value.callValue("copy", CPythonLanguage.NoType)
                        )
                        local(output)
                        output
                    }

                    val color = lineParams.color
                    val thickness = lineParams.thickness.value

                    val colorScalar = CPythonLanguage.tuple(color.a.v, color.b.v, color.c.v, color.d.v)

                    // TODO: Implement rotated rect drawing in python

                    fun ScopeContext.runtimeRect(rectValue: Value) {
                        val rectangle = CPythonLanguage.tupleVariables(
                            rectValue,
                            "x", "y", "w", "h"
                        )
                        local(rectangle)

                        // cv2.rectangle(mat, (x, y), (x + w, y + h), color, thickness)
                        // color is a (r, g, b, a) tuple
                        cv2(
                            "rectangle", target,
                            CPythonLanguage.tuple(rectangle.get("x"), rectangle.get("y")),
                            CPythonLanguage.tuple(
                                rectangle.get("x") + rectangle.get("w"),
                                rectangle.get("y") + rectangle.get("h")
                            ),
                            colorScalar,
                            thickness.v
                        )
                    }

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rect) {
                                cv2(
                                    "rectangle", target,
                                    CPythonLanguage.tuple(rectangle.x.value.v, rectangle.y.value.v),
                                    CPythonLanguage.tuple(
                                        rectangle.x.value.v + rectangle.w.value.v,
                                        rectangle.y.value.v + rectangle.h.value.v
                                    ),
                                    colorScalar,
                                    thickness.v
                                )
                            } else if (rectangle is GenValue.GRect.RuntimeRect) {
                                runtimeRect(rectangle.value)
                            }
                        }
                    } else {
                        foreach(variable(CPythonLanguage.NoType, "rect"), rectanglesList.value) { rect ->
                            runtimeRect(rect)
                        }
                    }

                    session.outputMat = GenValue.Mat(target, input.color, input.isBinary)
                }
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if (attrib == outputMat) {
            return current.sessionOf(this)!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}