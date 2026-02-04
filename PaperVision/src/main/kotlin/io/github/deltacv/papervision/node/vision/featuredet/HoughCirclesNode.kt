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

package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.math.RangeAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.CircleAttribute
import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.util.Range2d

@PaperNode(
    name = "nod_houghcircles",
    category = Category.FEATURE_DET,
    description = "des_houghcircles"
)
class HoughCirclesNode : DrawNode<HoughCirclesNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    val minDistance = DoubleAttribute(INPUT, "$[att_mindistance]", 1.0)

    val radiusRange = RangeAttribute(INPUT, "$[att_radiusrange]", 0, 300)

    val downscale = DoubleAttribute(INPUT, "$[att_downscale]")

    val param1 = DoubleAttribute(INPUT, "$[att_edgethreshold]", 255.0)
    val param2 = DoubleAttribute(INPUT, "$[att_accumulatorthreshold]", 100.0)

    val output = ListAttribute(OUTPUT, CircleAttribute, "$[att_circles]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + minDistance
        minDistance.fieldMode(Range2d(0.1, Double.MAX_VALUE))

        + radiusRange
        radiusRange.useSliders = false

        + downscale
        downscale.fieldMode(Range2d(0.1, Double.MAX_VALUE))

        + param1
        param1.fieldMode(Range2d(1.0, Double.MAX_VALUE))

        + param2
        param2.fieldMode(Range2d(1.0, Double.MAX_VALUE))

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            // Circle type needs to be lazily evaluated to gen its inner class
            val Circle = JvmOpenCvTypes.getCircleType(current)

            val session = Session()

            val inputValue = input.genValue(current).value

            val minDistanceValue = minDistance.genValue(current).value

            val minRadiusValue = radiusRange.genValue(current).min.value
            val maxRadiusValue = radiusRange.genValue(current).max.value

            val param1Value = param1.genValue(current).value
            val param2Value = param2.genValue(current).value

            val downscaleValue = downscale.genValue(current).value

            current {
                val circlesMatVar = uniqueVariable("houghCirclesMat", JvmOpenCvTypes.Mat.new())
                val circlesListVar = uniqueVariable("houghCirclesList",
                    JavaTypes.ArrayList(Circle).new()
                )

                val minDistanceVar = uniqueVariable("houghCirclesMinDistance", minDistanceValue.v)

                val minRadiusVar = uniqueVariable("houghCirclesMinRadius", int(minRadiusValue.v))
                val maxRadiusVar = uniqueVariable("houghCirclesMaxRadius", int(maxRadiusValue.v))

                val param1Var = uniqueVariable("houghCirclesParam1", param1Value.v)
                val param2Var = uniqueVariable("houghCirclesParam2", param2Value.v)

                val downscaleVar = uniqueVariable("houghCirclesDownscale", downscaleValue.v)

                group {
                    private(circlesMatVar)
                    private(circlesListVar)

                    public(minDistanceVar, minDistance.label())
                    public(minRadiusVar, radiusRange.label(0))
                    public(maxRadiusVar, radiusRange.label(1))
                    public(param1Var, param1.label())
                    public(param2Var, param2.label())
                    public(downscaleVar, downscale.label())
                }

                current.scope {
                    nameComment()

                    circlesMatVar("release")
                    circlesListVar("clear")

                    separate()

                    JvmOpenCvTypes.Imgproc("HoughCircles",
                        inputValue.v,
                        circlesMatVar,
                        JvmOpenCvTypes.Imgproc.HOUGH_GRADIENT,
                        downscaleVar,
                        minDistanceVar,
                        param1Var,
                        param2Var,
                        minRadiusVar,
                        maxRadiusVar
                    )

                    separate()

                    // Convert Mat to List<Circle>
                    forLoop(AccessorVariable(IntType, "x"), 0.v, circlesMatVar.callValue("cols", IntType)) {
                        val circle = DeclarableVariable("circle", circlesMatVar.callValue("get", DoubleType.arrayType(), 0.v, it))
                        local(circle)

                        separate()

                        val p = DeclarableVariable("p",
                            JvmOpenCvTypes.Point.new(
                                int(circle[0.v, DoubleType]),
                                int(circle[1.v, DoubleType])
                            )
                        )
                        local(p)

                        val c = DeclarableVariable("c", int(circle[2.v, DoubleType]))
                        local(c)

                        separate()

                        circlesListVar("add", Circle.new(p, c))
                    }
                }

                session.circles = GenValue.GList.RuntimeListOf(circlesListVar.resolved(), GenValue.GCircle.RuntimeCircle::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val input = input.genValue(current).value

            val minDistanceValue = minDistance.genValue(current).value

            val minRadiusValue = radiusRange.genValue(current).min.value
            val maxRadiusValue = radiusRange.genValue(current).max.value

            val param1Value = param1.genValue(current).value
            val param2Value = param2.genValue(current).value

            val downscaleValue = downscale.genValue(current).value

            current {
                current.scope {
                    val circles = uniqueVariable("hough_circles",
                        CPythonOpenCvTypes.cv2.callValue("HoughCircles",
                            CPythonLanguage.NoType,
                            input.v,
                            CPythonOpenCvTypes.cv2.HOUGH_GRADIENT,
                            downscaleValue.v,
                            minDistanceValue.v,
                            param1Value.v,
                            param2Value.v,
                            minRadiusValue.v,
                            maxRadiusValue.v
                        )
                    )

                    local(circles)

                    ifCondition(CPythonLanguage.valueIsNot(circles, CPythonLanguage.NoType)) {
                        circles set circles[csv(0.v, CPythonLanguage.sliceValue()), CPythonLanguage.NoType] // "[0, :]"
                    }

                    session.circles = GenValue.GList.RuntimeListOf(circles.resolved(), GenValue.GCircle.RuntimeCircle::class.resolved())
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when (attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.circles }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var circles: GenValue.GList.RuntimeListOf<GenValue.GCircle.RuntimeCircle>
    }
}
