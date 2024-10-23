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
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
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

    override val generators = generators {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val input = contours.value(current)

                if(input !is GenValue.GList.RuntimeListOf<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val points2f = uniqueVariable("${input.value.value}2f", JvmOpenCvTypes.MatOfPoint2f.new())
                val rectsList = uniqueVariable("${input.value.value}RotRects", JavaTypes.ArrayList(JvmOpenCvTypes.RotatedRect).new())

                group {
                    private(points2f)
                    private(rectsList)
                }

                current.scope {
                    rectsList("clear")

                    foreach(variable(JvmOpenCvTypes.MatOfPoint, "points"), input.value) {
                        points2f("release")
                        it("convertTo", points2f, cvTypeValue("CV_32F"))

                        separate()

                        rectsList("add", Imgproc.callValue("minAreaRect", JvmOpenCvTypes.RotatedRect, points2f))
                    }
                }

                session.rects = GenValue.GList.RuntimeListOf(rectsList, GenValue.GRect.Rotated.RuntimeRotatedRect::class)
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            outputRects -> current.session(this)!!.rects
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var rects: GenValue.GList.RuntimeListOf<GenValue.GRect.Rotated.RuntimeRotatedRect>
    }
}