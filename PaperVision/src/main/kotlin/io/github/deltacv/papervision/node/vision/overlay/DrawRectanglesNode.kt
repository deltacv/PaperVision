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
import io.github.deltacv.papervision.codegen.build.language.cpython.CPythonOpenCv.cv2
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv.Imgproc
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv.Mat
import io.github.deltacv.papervision.codegen.dsl.ScopeCtx
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_drawrects",
    category = NodeCategory.OVERLAY,
    description = "des_drawrects"
)
@CodecType
open class DrawRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false) : DrawNode<DrawRectanglesNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val rectangles = ListAttribute(INPUT, "$[att_rects]", RectAttribute)

    val lineParams = LineParametersAttribute(INPUT, "$[att_params]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        +inputMat.rebuildOnChange()

        +lineParams

        +rectangles.rebuildOnChange()

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

                val lineParams = JvmOpenCv.toRuntimeLineParameters(lineParams.genValue(current), current)

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

                    if (rectanglesList !is GenValue.List.Runtime<*>) {
                        for (rectangle in (rectanglesList as GenValue.List.Actual<*>).elements) {
                            if (rectangle is GenValue.Rect.Components) {
                                val inst = JvmOpenCv.toRectInst(rectangle, current).value.v

                                val nullables = findNullables(inst)

                                ifCondition(nullables.joinWithAnd { it notEqualsTo language.nullValue }) {
                                    Imgproc(
                                        "rectangle", drawMat,
                                        inst,
                                        lineParams.color.value.v,
                                        lineParams.thicknessValue.v
                                    )
                                }
                            } else if (rectangle is GenValue.Rect.Inst) {
                                ifCondition(
                                    rectangle.value.v notEqualsTo language.nullValue and
                                            (drawMat notEqualsTo language.nullValue) and
                                            not(drawMat.callValue("empty", BooleanType).condition())
                                ) {
                                    Imgproc(
                                        "rectangle", drawMat, rectangle.value.v,
                                        lineParams.color.value.v, lineParams.thicknessValue.v
                                    )
                                }
                            }
                        }
                    } else {
                        ifCondition(
                            (drawMat notEqualsTo language.nullValue) and
                                    not(drawMat.callValue("empty", BooleanType).condition())
                        ) {
                            foreach(variable(JvmOpenCv.Rect, "rect"), rectanglesList.value.v) {
                                Imgproc(
                                    "rectangle", drawMat, it,
                                    lineParams.color.value.v, lineParams.thicknessValue.v
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

                val lineParams = lineParams.genValue(current) as GenValue.LineParameters.Actual

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
                        CPythonLanguage.tuple(color.a.v, color.b.v, color.c.v, color.d.v)

                    fun ScopeCtx.runtimeRect(rectValue: Value) {
                        ifCondition(rectValue notEqualsTo language.nullValue) {
                            val rectangle = CPythonLanguage.declaredTupleVariable(
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

                    if (rectanglesList !is GenValue.List.Runtime<*>) {
                        for (rectangle in (rectanglesList as GenValue.List.Actual<*>).elements) {
                            if (rectangle is GenValue.Rect.Components) {
                                val pos = rectangle.position.toRuntime(current)
                                val size = rectangle.size.toRuntime(current)

                                val tl = CPythonLanguage.tuple(
                                    pos.xValue.v, pos.yValue.v
                                )
                                val br = CPythonLanguage.tuple(
                                    pos.xValue.v + size.xValue.v,
                                    pos.yValue.v + size.yValue.v
                                )

                                val nullables = findNullables(tl, br)

                                ifCondition(nullables.joinWithAnd { it notEqualsTo nullValue }) {
                                    cv2(
                                        "rectangle", target,
                                        tl, br,
                                        colorScalar,
                                        thickness.v
                                    )
                                }
                            } else if (rectangle is GenValue.Rect.Inst) {
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

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("inputMat", inputMat)
        encoder.obj("rectangles", rectangles)
        encoder.obj("lineParams", lineParams)
        if (!isDrawOnInput) {
            encoder.obj("outputMat", outputMat)
        }
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("inputMat", inputMat)
        decoder.obj("rectangles", rectangles)
        decoder.obj("lineParams", lineParams)
        if (!isDrawOnInput) {
            decoder.obj("outputMat", outputMat)
        }
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@PaperNode(
    name = "nod_drawrects_onimage",
    category = NodeCategory.OVERLAY,
    description = "des_drawrects_onimage",
    showInList = false // executive decision
)
class DrawRectanglesOnImageNode : DrawRectanglesNode(true)
