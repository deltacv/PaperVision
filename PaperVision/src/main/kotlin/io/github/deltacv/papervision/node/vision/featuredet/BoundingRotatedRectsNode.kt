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

package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_boundingrotated_rect",
    category = Category.FEATURE_DET,
    description = "des_boundingrotated_rect"
)
class BoundingRotatedRectsNode : DrawNode<BoundingRotatedRectsNode.Session>() {

    val contours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val outputRects = ListAttribute(OUTPUT, RotatedRectAttribute, "$[att_rects]")

    override fun onEnable() {
        + contours.rebuildOnChange()
        + outputRects.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val input = contours.value(current)

                val name = if(input is GenValue.GList.RuntimeListOf<*>) {
                    input.value.value.toString()
                } else null

                val points2f = uniqueVariable("${name ?: "points"}2f", JvmOpenCvTypes.MatOfPoint2f.new())
                val rectsList = uniqueVariable("${name?.run { this + "R"} ?: "r"}otRects", JavaTypes.ArrayList(JvmOpenCvTypes.RotatedRect).new())

                group {
                    private(points2f)
                    private(rectsList)
                }

                current.scope {
                    writeNameComment()

                    rectsList("clear")

                    fun ScopeContext.withPoints(points: Value) {
                        points2f("release")
                        points("convertTo", points2f, cvTypeValue("CV_32F"))

                        separate()

                        rectsList("add", Imgproc.callValue("minAreaRect", JvmOpenCvTypes.RotatedRect, points2f))
                    }

                    if(input is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(JvmOpenCvTypes.MatOfPoint, "points"), input.value.v) {
                            withPoints(it)
                        }
                    } else {
                        for(element in (input as GenValue.GList.ListOf<*>).elements) {
                            if(element is GenValue.GPoints.RuntimePoints) {
                                withPoints(element.value.v)
                            } else {
                                raise("Invalid input type for contours")
                            }
                        }
                    }
                }

                session.rects = GenValue.GList.RuntimeListOf(rectsList.resolved(), GenValue.GRect.Rotated.RuntimeRotatedRect::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val input = contours.value(current)

                val name = if(input is GenValue.GList.RuntimeListOf<*>) {
                    input.value.value.toString() + "_r"
                } else null

                val rectsList = uniqueVariable("${name ?: "r"}ot_rects", CPythonLanguage.NoType.newArray())

                current.scope {
                    writeNameComment()

                    local(rectsList)

                    fun ScopeContext.withPoints(points: Value) {
                        rectsList("append", cv2.callValue("minAreaRect", CPythonLanguage.NoType, points))
                    }

                    if(input is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(CPythonLanguage.NoType, "points"), input.value.v) {
                            withPoints(it)
                        }
                    } else {
                        for(element in (input as GenValue.GList.ListOf<*>).elements) {
                            if(element is GenValue.GPoints.RuntimePoints) {
                                withPoints(element.value.v)
                            } else {
                                raise("Invalid input type for contours")
                            }
                        }
                    }
                }

                session.rects = GenValue.GList.RuntimeListOf(rectsList.resolved(), GenValue.GRect.Rotated.RuntimeRotatedRect::class.resolved())
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            outputRects -> current.nonNullSessionOf(this).rects
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var rects: GenValue.GList.RuntimeListOf<GenValue.GRect.Rotated.RuntimeRotatedRect>
    }
}