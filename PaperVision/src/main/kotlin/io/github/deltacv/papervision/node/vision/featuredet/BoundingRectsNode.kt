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
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_boundingrect",
    category = Category.FEATURE_DET,
    description = "des_boundingrect"
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>() {

    val inputContours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val outputRects = ListAttribute(OUTPUT, RectAttribute, "$[att_boundingrects]")

    override fun onEnable() {
        + inputContours.rebuildOnChange()
        + outputRects.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputContours.genValue(current)

                val listName = if (input is GenValue.GList.RuntimeListOf<*>) {
                    "${input.value.value}Rects"
                } else "rects"

                val rectsList = uniqueVariable(listName, JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())

                group {
                    private(rectsList)
                }

                current.scope {
                    nameComment()

                    rectsList("clear")

                    if (input is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(JvmOpenCvTypes.MatOfPoint, "points"), input.value.v) {
                            rectsList("add", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, it))
                        }
                    } else {
                        for (points in (input as GenValue.GList.ListOf<*>).elements) {
                            if (points is GenValue.GPoints.RuntimePoints) {
                                ifCondition(points.value.v notEqualsTo language.nullValue) {
                                    rectsList(
                                        "add",
                                        Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, points.value.v)
                                    )
                                }
                            } else {
                                raise("Invalid input type for contours")
                            }
                        }
                    }
                }

                session.outputRects = GenValue.GList.RuntimeListOf(rectsList.resolved(), GenValue.GRect.RuntimeRect::class.resolved())

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val input = inputContours.genValue(current)

                val listName = if (input is GenValue.GList.RuntimeListOf<*>) {
                    "${input.value.value}_rects"
                } else "rects"

                val rectsList = uniqueVariable(listName, CPythonLanguage.NoType.newArray(0.v))

                current.scope {
                    nameComment()

                    local(rectsList)

                    if (input is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(CPythonLanguage.NoType, "points"), input.value.v) { points ->
                            rectsList("append", cv2.callValue("boundingRect", CPythonLanguage.NoType, points))
                        }
                    } else {
                        for (points in (input as GenValue.GList.ListOf<*>).elements) {
                            if (points is GenValue.GPoints.RuntimePoints) {
                                ifCondition(points.value.v notEqualsTo language.nullValue) {
                                    rectsList(
                                        "append",
                                        cv2.callValue("boundingRect", CPythonLanguage.NoType, points.value.v)
                                    )
                                }
                            } else {
                                raise("Invalid input type for contours")
                            }
                        }
                    }
                }

                session.outputRects = GenValue.GList.RuntimeListOf(rectsList.resolved(), GenValue.GRect.RuntimeRect::class.resolved())

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if (attrib == outputRects) {
            return GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.outputRects }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputRects: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }

}