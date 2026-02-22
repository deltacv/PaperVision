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
import io.github.deltacv.papervision.codegen.build.language.cpython.CPythonOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.JavaTypes
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
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
    name = "nod_groupcontours_byarea",
    category = NodeCategory.CLASSIFICATION,
    description = "des_groupcontours_byarea"
)
@CodecType
class FilterContoursByAreaNode : DrawNode<FilterContoursByAreaNode.Session>() {

    val input = ListAttribute(INPUT, "$[att_contours]", PointsAttribute)

    val minArea = IntAttribute(INPUT, "$[att_minarea]")
    val maxArea = IntAttribute(INPUT, "$[att_maxarea]")

    val output = ListAttribute(OUTPUT, "$[att_filteredcontours]", PointsAttribute)

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

            if(contours !is GenValue.List.Runtime<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minAreaVal = minArea.genValue(current)
            val maxAreaVal = maxArea.genValue(current)

            current {
                val minAreaVar = uniqueVariable("minArea", minAreaVal.v)
                val maxAreaVar = uniqueVariable("maxArea", maxAreaVal.v)

                val contoursVar = uniqueVariable("${contours.value.v}ByArea", JavaTypes.ArrayList(JvmOpenCv.MatOfPoint).new())

                group {
                    public(minAreaVar, minArea.label())
                    public(maxAreaVar, maxArea.label())

                    private(contoursVar)
                }

                current.scope {
                    nameComment()

                    contoursVar("clear")

                    foreach(AccessorVariable(JvmOpenCv.MatOfPoint, "contour"), contours.value.v) { contour ->
                        val areaVar = uniqueVariable("area", JvmOpenCv.Imgproc.callValue("contourArea", DoubleType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan minAreaVar) and (areaVar lessOrEqualThan maxAreaVar)) {
                            contoursVar("add", contour)
                        }
                    }
                }

                session.output = GenValue.List.Runtime<GenValue.Points.Runtime>(contoursVar.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val contours = input.genValue(current)

            if(contours !is GenValue.List.Runtime<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            current {
                val minArea = minArea.genValue(current)
                val maxArea = maxArea.genValue(current)

                val contoursVar = uniqueVariable("by_area_contours", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))

                current.scope {
                    local(contoursVar)

                    foreach(AccessorVariable(CPythonLanguage.NoType, "contour"), contours.value.v) { contour ->
                        val areaVar = uniqueVariable("area", CPythonOpenCv.cv2.callValue("contourArea", CPythonLanguage.NoType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan int(minArea).v) and (areaVar lessOrEqualThan int(maxArea).v)) {
                            contoursVar("append", contour)
                        }
                    }
                }

                session.output = GenValue.List.Runtime(contoursVar.resolved(), GenValue.Points.Runtime::class.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> GenValue.List.Runtime.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("minArea", minArea)
        encoder.obj("maxArea", maxArea)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("input", input)
        decoder.obj("minArea", minArea)
        decoder.obj("maxArea", maxArea)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.List.Runtime<*>
    }

}
