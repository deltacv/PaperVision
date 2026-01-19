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
import io.github.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
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
    name = "nod_filterbiggest_rotrect",
    category = Category.FEATURE_DET,
    description = "des_filterbiggest_rotrect"
)
class FilterBiggestRotatedRectangleNode : DrawNode<FilterBiggestRotatedRectangleNode.Session>() {

    val input = ListAttribute(INPUT, RotatedRectAttribute, "$[att_rotrects]")
    val output = RotatedRectAttribute(OUTPUT, "$[att_biggestrot_rect]")

    override fun onEnable() {
        +input.rebuildOnChange()
        +output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val rectsList = input.genValue(current)

                val biggestRect = uniqueVariable("biggestRotRect", JvmOpenCvTypes.RotatedRect.nullValue)

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
                                        (rect.propertyValue("size", JvmOpenCvTypes.Size).callValue("area", DoubleType)
                                                greaterThan biggestRect.propertyValue("size", JvmOpenCvTypes.Size).callValue("area", DoubleType)
                                                )
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if (rectsList is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(JvmOpenCvTypes.RotatedRect, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.GList.ListOf<*>).elements) {
                            if (element is GenValue.GRect.Rotated.RotatedRect) {
                                separate()
                                val rect = DeclarableVariable(
                                    "rect",
                                    JvmOpenCvTypes.RotatedRect.new(
                                        JvmOpenCvTypes.Point.new(element.x.value.v, element.y.value.v),
                                        JvmOpenCvTypes.Size.new(element.w.value.v, element.h.value.v),
                                        element.angle.value.v
                                    )
                                )

                                local(rect)

                                ifCondition(
                                    biggestRect equalsTo biggestRect.nullValue or (rect.callValue(
                                        "area",
                                        DoubleType
                                    ) greaterThan biggestRect.callValue("area", DoubleType))
                                ) {
                                    biggestRect instanceSet rect
                                }
                            } else if (element is GenValue.GRect.RuntimeRect) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.GRect.Rotated.RuntimeRotatedRect(biggestRect.resolved())

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
                            val rectArea = rect[1.v, CPythonLanguage.NoType][0.v, CPythonLanguage.NoType] * rect[1.v, CPythonLanguage.NoType][1.v, CPythonLanguage.NoType]
                            val biggestRectArea =
                                biggestRect[1.v, CPythonLanguage.NoType][0.v, CPythonLanguage.NoType] * biggestRect[1.v, CPythonLanguage.NoType][1.v, CPythonLanguage.NoType]

                            ifCondition(
                                biggestRect equalsTo CPythonLanguage.nullValue or (rectArea greaterThan biggestRectArea)
                            ) {
                                biggestRect instanceSet rect
                            }
                        }
                    }

                    if (rectsList is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(CPythonLanguage.NoType, "rect"), rectsList.value.v) { rect ->
                            withRuntimeRect(rect)
                        }
                    } else {
                        for (element in (rectsList as GenValue.GList.ListOf<*>).elements) {
                            if (element is GenValue.GRect.Rotated.RotatedRect) {
                                separate()

                                val rect = uniqueVariable(
                                    "rect",
                                    CPythonLanguage.tuple(
                                        CPythonLanguage.tuple(element.x.value.v, element.y.value.v),
                                        CPythonLanguage.tuple(element.w.value.v, element.h.value.v),
                                        element.angle.value.v
                                    )
                                )
                                local(rect)

                                val rectArea = rect[2.v, CPythonLanguage.NoType] * rect[3.v, CPythonLanguage.NoType]
                                val biggestRectArea =
                                    biggestRect[2.v, CPythonLanguage.NoType] * biggestRect[3.v, CPythonLanguage.NoType]

                                ifCondition(
                                    biggestRect equalsTo CPythonLanguage.nullValue or (rectArea greaterThan biggestRectArea)
                                ) {
                                    biggestRect instanceSet rect
                                }
                            } else if (element is GenValue.GRect.Rotated.RuntimeRotatedRect) {
                                separate()
                                withRuntimeRect(element.value.v)
                            }
                        }
                    }
                }

                session.biggestRect = GenValue.GRect.Rotated.RuntimeRotatedRect(biggestRect.resolved())

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if (attrib == output) {
            return GenValue.GRect.Rotated.RuntimeRotatedRect.defer { current.sessionOf(this)?.biggestRect }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var biggestRect: GenValue.GRect.Rotated.RuntimeRotatedRect
    }

}