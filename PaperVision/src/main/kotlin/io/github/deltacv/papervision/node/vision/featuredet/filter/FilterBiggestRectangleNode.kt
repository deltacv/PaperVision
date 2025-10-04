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

package io.github.deltacv.papervision.node.vision.featuredet.filter

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_filterbiggest_rect",
    category = Category.FEATURE_DET,
    description = "des_filterbiggest_rect"
)
class FilterBiggestRectangleNode : DrawNode<FilterBiggestRectangleNode.Session>() {

    val input = ListAttribute(INPUT, RectAttribute, "$[att_rects]")
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

                val biggestRect = uniqueVariable("biggestRect", JvmOpenCvTypes.Rect.nullValue)

                group {
                    private(biggestRect)
                }

                current.scope {
                    nameComment()

                    biggestRect instanceSet biggestRect.nullValue

                    fun ScopeContext.withRuntimeRect(rect: Value) {
                        ifCondition(rect notEqualsTo language.nullValue) {
                            ifCondition(
                                biggestRect equalsTo biggestRect.nullValue or
                                        (rect.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if(rectsList is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(JvmOpenCvTypes.Rect, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.GList.ListOf<*>).elements) {
                            if(element is GenValue.GRect.Rect) {
                                separate()
                                val rect = Variable(
                                    "rect",
                                    JvmOpenCvTypes.Rect.new(
                                        element.x.value.v,
                                        element.y.value.v,
                                        element.w.value.v,
                                        element.h.value.v
                                    )
                                )

                                local(rect)

                                ifCondition(
                                    biggestRect equalsTo biggestRect.nullValue or (rect.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                                ) {
                                    biggestRect instanceSet rect
                                }
                            } else if(element is GenValue.GRect.RuntimeRect) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.GRect.RuntimeRect(biggestRect.resolved())

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val rectsList = input.genValue(current)

                val biggestRect = uniqueVariable("biggest_rect", CPythonLanguage.nullValue)

                current.scope {
                    nameComment()

                    local(biggestRect)

                    fun ScopeContext.withRuntimeRect(rect: Value) {
                        ifCondition(rect notEqualsTo language.nullValue) {
                            // 2 - width, 3 - height, of tuple (x, y, w, h)
                            val rectArea = rect[2.v, CPythonLanguage.NoType] * rect[3.v, CPythonLanguage.NoType]
                            val biggestRectArea = biggestRect[2.v, CPythonLanguage.NoType] * biggestRect[3.v, CPythonLanguage.NoType]

                            ifCondition(
                                biggestRect equalsTo CPythonLanguage.nullValue or (rectArea greaterThan biggestRectArea)
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if(rectsList is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(CPythonLanguage.NoType, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.GList.ListOf<*>).elements) {
                            if(element is GenValue.GRect.Rect) {
                                separate()

                                val rect = uniqueVariable(
                                    "rect",
                                    CPythonLanguage.tuple(
                                        element.x.value.v,
                                        element.y.value.v,
                                        element.w.value.v,
                                        element.h.value.v
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
                            } else if(element is GenValue.GRect.RuntimeRect) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.GRect.RuntimeRect(biggestRect.resolved())

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return GenValue.GRect.RuntimeRect.defer { current.sessionOf(this)?.biggestRect }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var biggestRect: GenValue.GRect.RuntimeRect
    }

}