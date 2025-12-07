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
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
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
    name = "nod_drawrects",
    category = Category.OVERLAY,
    description = "des_drawrects"
)
open class DrawRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false) : DrawNode<DrawRectanglesNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val rectangles = ListAttribute(INPUT, RectAttribute, "$[att_rects]")

    val lineParams = LineParametersAttribute(INPUT, "$[att_params]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + lineParams

        + rectangles.rebuildOnChange()

        if (!isDrawOnInput) {
            +outputMat.enablePrevizButton().rebuildOnChange()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val lineParams = (lineParams.genValue(current) as GenValue.LineParameters).ensureRuntimeLineJava(current)

                val input = inputMat.genValue(current)
                val rectanglesList = rectangles.genValue(current)
                val output = uniqueVariable("${input.value.v}Rects", Mat.new())

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

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rect) {
                                Imgproc(
                                    "rectangle", drawMat,
                                    JvmOpenCvTypes.Rect.new(
                                        double(rectangle.x.value.v), double(rectangle.y.value.v),
                                        double(rectangle.w.value.v), double(rectangle.h.value.v)
                                    ),
                                    lineParams.colorScalarValue.v,
                                    lineParams.thicknessValue.v
                                )
                            } else if (rectangle is GenValue.GRect.RuntimeRect) {
                                ifCondition(
                                    rectangle.value.v notEqualsTo language.nullValue and
                                            (drawMat notEqualsTo language.nullValue) and
                                            not(drawMat.callValue("empty", BooleanType).condition())
                                ) {
                                    Imgproc(
                                        "rectangle", drawMat, rectangle.value.v,
                                        lineParams.colorScalarValue.v, lineParams.thicknessValue.v
                                    )
                                }
                            }
                        }
                    } else {
                        ifCondition(
                            (drawMat notEqualsTo language.nullValue) and
                                    not(drawMat.callValue("empty", BooleanType).condition())
                        ) {
                            foreach(variable(JvmOpenCvTypes.Rect, "rect"), rectanglesList.value.v) {
                                Imgproc(
                                    "rectangle", drawMat, it,
                                    lineParams.colorScalarValue.v, lineParams.thicknessValue.v
                                )
                            }
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

                val lineParams = lineParams.genValue(current)
                if (lineParams !is GenValue.LineParameters.Line) {
                    raise("Line parameters must not be runtime")
                }

                current.scope {
                    nameComment()

                    val target = if (isDrawOnInput) {
                        input.value.v
                    } else {
                        val output = uniqueVariable(
                            "${input.value.v}_rects",
                            input.value.v.callValue("copy", CPythonLanguage.NoType)
                        )
                        local(output)
                        output
                    }

                    val color = lineParams.color
                    val thickness = lineParams.thickness.value

                    val colorScalar =
                        CPythonLanguage.tuple(color.a.value.v, color.b.value.v, color.c.value.v, color.d.value.v)

                    fun ScopeContext.runtimeRect(rectValue: Value) {
                        ifCondition(rectValue notEqualsTo language.nullValue) {
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

@PaperNode(
    name = "nod_drawrects_onimage",
    category = Category.OVERLAY,
    description = "des_drawrects_onimage",
    showInList = false // executive decision
)
class DrawRectanglesOnImageNode : DrawRectanglesNode(true)