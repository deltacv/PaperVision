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

package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_groupcontours_byarea",
    category = Category.CLASSIFICATION,
    description = "des_groupcontours_byarea"
)
class FilterContoursByAreaNode : DrawNode<FilterContoursByAreaNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val minArea = IntAttribute(INPUT, "$[att_minarea]")
    val maxArea = IntAttribute(INPUT, "$[att_maxarea]")

    val output = ListAttribute(OUTPUT, PointsAttribute, "$[att_filteredcontours]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + minArea
        + maxArea

        maxArea.value.set(100)

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val contours = input.genValue(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minAreaVal = minArea.genValue(current)
            val maxAreaVal = maxArea.genValue(current)

            current {
                val minAreaVar = uniqueVariable("minArea", minAreaVal.value.v)
                val maxAreaVar = uniqueVariable("maxArea", maxAreaVal.value.v)

                val contoursVar = uniqueVariable("${contours.value.v}ByArea", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())

                group {
                    public(minAreaVar, minArea.label())
                    public(maxAreaVar, maxArea.label())

                    private(contoursVar)
                }

                current.scope {
                    nameComment()

                    contoursVar("clear")

                    foreach(AccessorVariable(JvmOpenCvTypes.MatOfPoint, "contour"), contours.value.v) { contour ->
                        val areaVar = uniqueVariable("area", JvmOpenCvTypes.Imgproc.callValue("contourArea", DoubleType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan minAreaVar) and (areaVar lessOrEqualThan maxAreaVar)) {
                            contoursVar("add", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf<GenValue.GPoints.RuntimePoints>(contoursVar.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val contours = input.genValue(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            current {
                val minArea = minArea.genValue(current).value.v
                val maxArea = maxArea.genValue(current).value.v

                val contoursVar = uniqueVariable("by_area_contours", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))

                current.scope {
                    local(contoursVar)

                    foreach(AccessorVariable(CPythonLanguage.NoType, "contour"), contours.value.v) { contour ->
                        val areaVar = uniqueVariable("area", CPythonOpenCvTypes.cv2.callValue("contourArea", CPythonLanguage.NoType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan minArea) and (areaVar lessOrEqualThan maxArea)) {
                            contoursVar("append", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(contoursVar.resolved(), GenValue.GPoints.RuntimePoints::class.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<*>
    }

}
