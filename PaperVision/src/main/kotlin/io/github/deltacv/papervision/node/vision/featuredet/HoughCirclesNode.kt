/*
 * PaperVision
 * Copyright (C) 2025 Sebastian Erives, deltacv

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
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.CircleAttribute
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
    name = "nod_houghcircles",
    category = Category.FEATURE_DET,
    description = "des_houghcircles"
)
class HoughCirclesNode : DrawNode<HoughCirclesNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    val minDistance = DoubleAttribute(INPUT, "$[att_mindistance]")
    val downscale = DoubleAttribute(INPUT, "$[att_downscale]")

    val output = ListAttribute(OUTPUT, CircleAttribute, "$[att_calccircles]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + minDistance
        minDistance.value.set(10.0)
        + downscale

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            // Circle type needs to be lazily evaluated to gen its inner class
            val Circle = JvmOpenCvTypes.getCircleType(current)

            val session = Session()

            val inputValue = input.genValue(current).value

            val minDistanceValue = minDistance.genValue(current).value
            val downscaleValue = downscale.genValue(current).value

            current {
                val circlesMatVar = uniqueVariable("houghCirclesMat", JvmOpenCvTypes.Mat.new())
                val circlesListVar = uniqueVariable("houghCirclesList",
                    JavaTypes.ArrayList(Circle).new()
                )

                val downscaleVar = uniqueVariable("houghCirclesDownscale", downscaleValue.v)
                val minDistanceVar = uniqueVariable("houghCirclesMinDistance", minDistanceValue.v)

                group {
                    private(circlesMatVar)
                    private(circlesListVar)

                    public(downscaleVar, downscale.label())
                    public(minDistanceVar, minDistance.label())
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
                        minDistanceVar
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
            val downscaleValue = downscale.genValue(current).value

            current {
                current.scope {
                    val circles = uniqueVariable("hough_circles",
                        CPythonOpenCvTypes.cv2.callValue("HoughCircles",
                            CPythonLanguage.NoType,
                            input.v,
                            CPythonOpenCvTypes.cv2.HOUGH_GRADIENT,
                            downscaleValue.v,
                            minDistanceValue.v
                        )
                    )

                    local(circles)

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