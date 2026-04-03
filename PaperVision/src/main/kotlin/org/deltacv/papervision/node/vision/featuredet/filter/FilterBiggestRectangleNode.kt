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

package org.deltacv.papervision.node.vision.featuredet.filter

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.misc.ListAttribute
import org.deltacv.papervision.attribute.rebuildOnChange
import org.deltacv.papervision.attribute.vision.structs.RectAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.Value
import org.deltacv.papervision.codegen.build.DeclarableVariable
import org.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import org.deltacv.papervision.codegen.dsl.ScopeCtx
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import org.deltacv.papervision.codegen.language.jvm.JavaLanguage
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_filterbiggest_rect",
    category = NodeCategory.CLASSIFICATION,
    description = "des_filterbiggest_rect"
)
@CodecType
class FilterBiggestRectangleNode : DrawNode<FilterBiggestRectangleNode.Session>() {

    val input = ListAttribute(INPUT, "$[att_rects]", RectAttribute)
    val output = RectAttribute(OUTPUT, "$[att_biggestrect]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val rectsList = input.genValue(current)

                val biggestRect = uniqueVariable("biggestRect", JvmOpenCv.Rect.nullValue, isNullable = true)

                group {
                    private(biggestRect)
                }

                current.scope {
                    nameComment()

                    biggestRect instanceSet biggestRect.nullValue

                    fun ScopeCtx.withRuntimeRect(rect: Value) {
                        ifCondition(rect notEqualsTo language.nullValue) {
                            ifCondition(
                                biggestRect equalsTo biggestRect.nullValue or
                                        (rect.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if(rectsList is GenValue.List.Runtime<*>) {
                        foreach(variable(JvmOpenCv.Rect, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.List.Actual<*>).elements) {
                            if(element is GenValue.Rect.Components) {
                                val pos = element.position.toRuntime(current)
                                val size = element.size.toRuntime(current)

                                separate()

                                val rect = DeclarableVariable(
                                    "rect",
                                    JvmOpenCv.Rect.new(
                                        pos.xValue.v,
                                        pos.yValue.v,
                                        size.xValue.v,
                                        size.yValue.v
                                    )
                                )

                                local(rect)

                                ifCondition(
                                    biggestRect equalsTo biggestRect.nullValue or (rect.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                                ) {
                                    biggestRect instanceSet rect
                                }
                            } else if(element is GenValue.Rect.Inst) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.Rect.Inst(biggestRect.resolved())

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val rectsList = input.genValue(current)

                val biggestRect = uniqueVariable("biggest_rect", CPythonLanguage.nullValue, isNullable = true)

                current.scope {
                    nameComment()

                    local(biggestRect)

                    fun ScopeCtx.withRuntimeRect(rect: Value) {
                        ifCondition(rect notEqualsTo language.nullValue) {
                            // index 2 for width, index 3 for height, of tuple (x, y, w, h)
                            val rectArea = rect[2.v, CPythonLanguage.NoType] * rect[3.v, CPythonLanguage.NoType]
                            val biggestRectArea = biggestRect[2.v, CPythonLanguage.NoType] * biggestRect[3.v, CPythonLanguage.NoType]

                            ifCondition(
                                biggestRect equalsTo CPythonLanguage.nullValue or (rectArea greaterThan biggestRectArea)
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if(rectsList is GenValue.List.Runtime<*>) {
                        foreach(variable(CPythonLanguage.NoType, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.List.Actual<*>).elements) {
                            if(element is GenValue.Rect.Components) {
                                separate()

                                val pos = element.position.toRuntime(current)
                                val size = element.size.toRuntime(current)

                                val rect = uniqueVariable(
                                    "rect",
                                    CPythonLanguage.tuple(
                                        pos.xValue.v,
                                        pos.yValue.v,
                                        size.xValue.v,
                                        size.yValue.v
                                    )
                                )
                                local(rect)

                                val rectArea = rect[2.v, CPythonLanguage.NoType] * rect[3.v, CPythonLanguage.NoType]
                                val biggestRectArea = biggestRect[2.v, CPythonLanguage.NoType] * biggestRect[3.v, CPythonLanguage.NoType]

                                ifCondition(
                                    biggestRect equalsTo CPythonLanguage.nullValue or (rectArea greaterThan biggestRectArea)
                                ) {
                                    biggestRect instanceSet rect
                                }
                            } else if(element is GenValue.Rect.Inst) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.Rect.Inst(biggestRect.resolved())

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == output) {
            return GenValue.Rect.Inst.defer { current.sessionOf(this)?.biggestRect }
        }

        noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("input", input)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var biggestRect: GenValue.Rect.Inst
    }

}



