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

package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
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
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_crosshair",
    category = Category.CLASSIFICATION,
    description = "des_crosshair"
)
class CrosshairNode : DrawNode<CrosshairNode.Session>() {

    enum class CrosshairPosition {
        Center, Offset, Custom
    }

    val drawCrosshairOn = MatAttribute(INPUT, "$[att_drawon_image]")
    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val crosshairPosition = EnumAttribute(INPUT, CrosshairPosition.values(), "$[att_position]")

    val crosshairScale = IntAttribute(INPUT, "$[att_scale]")

    val crosshairLineParams = LineParametersAttribute(INPUT, "$[att_crosshairline_params]")

    val outputCrosshair = ListAttribute(OUTPUT, PointsAttribute, "$[att_crosshair]")
    val outputCrosshairImage = MatAttribute(OUTPUT, "$[att_crosshairimage]")

    override fun onEnable() {
        + drawCrosshairOn.rebuildOnChange()
        + input.rebuildOnChange()
        + crosshairScale

        + crosshairPosition
        + crosshairLineParams

        crosshairPosition.onChange {
            when(crosshairPosition.get()) {
                CrosshairPosition.Center -> {
                }
                CrosshairPosition.Offset -> {
                }
                CrosshairPosition.Custom -> {
                }
            }
        }

        + outputCrosshairImage.enablePrevizButton().rebuildOnChange()
        + outputCrosshair.rebuildOnChange()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            val session = Session()

            val inputPoints = input.value(current)

            if (inputPoints !is GenValue.GList.RuntimeListOf<*>) {
                raise("") // TODO: Handle non-runtime lists
            }

            val drawOn = drawCrosshairOn.value(current)
            val drawOnValue = drawOn.value

            val crosshairPosValue = crosshairPosition.value(current).value

            val crosshairLineParams = (crosshairLineParams.value(current) as GenValue.LineParameters).ensureRuntimeLine(current)

            val crosshairSizeValue = crosshairScale.value(current).value

            current {
                val crosshair = uniqueVariable("crosshair", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())
                val crosshairImage = uniqueVariable("crosshairImage", JvmOpenCvTypes.Mat.new())
                val crosshairSize = uniqueVariable("crosshairSize", crosshairSizeValue.v)

                group {
                    private(crosshair)
                    private(crosshairImage)

                    public(crosshairSize, crosshairScale.label())
                }

                current.scope {
                    drawOnValue("copyTo", crosshairImage)

                    separate()

                    val crosshairPoint = uniqueVariable(
                        "crosshairPoint", if (crosshairPosValue == CrosshairPosition.Center) {
                            // draw crosshair at center
                            val rows = drawOnValue.callValue("rows", IntType)
                            val cols = drawOnValue.callValue("cols", IntType)

                            JvmOpenCvTypes.Point.new(double(cols) / 2.v, double(rows) / 2.v)
                        } else if (crosshairPosValue == CrosshairPosition.Offset) {
                            TODO()
                        } else {
                            TODO()
                        }
                    )

                    local(crosshairPoint)

                    val rows = drawOnValue.callValue("rows", IntType)
                    val cols = drawOnValue.callValue("cols", IntType)

                    // Define a scale factor based on the image dimensions
                    val scaleFactor = uniqueVariable("scaleFactor", (rows + cols) / 2.v)
                    local(scaleFactor)

                    separate()

                    // Adjust crosshairSize based on the scale factor
                    val adjustedCrosshairSize = uniqueVariable("adjustedCrosshairSize", crosshairSize * scaleFactor / 100.v)
                    local(adjustedCrosshairSize)

                    separate()

                    val crosshairCol = crosshairLineParams.colorScalarValue
                    val crosshairThickness = crosshairLineParams.thicknessValue

                    Imgproc(
                        "line",
                        crosshairImage,
                        JvmOpenCvTypes.Point.new(
                            crosshairPoint.propertyValue("x", DoubleType) - adjustedCrosshairSize,
                            crosshairPoint.propertyValue("y", DoubleType)
                        ),
                        JvmOpenCvTypes.Point.new(
                            crosshairPoint.propertyValue("x", DoubleType) + adjustedCrosshairSize,
                            crosshairPoint.propertyValue("y", DoubleType)
                        ),
                        crosshairCol,
                        crosshairThickness
                    )

                    Imgproc(
                        "line",
                        crosshairImage,
                        JvmOpenCvTypes.Point.new(
                            crosshairPoint.propertyValue("x", DoubleType),
                            crosshairPoint.propertyValue("y", DoubleType) - adjustedCrosshairSize
                        ),
                        JvmOpenCvTypes.Point.new(
                            crosshairPoint.propertyValue("x", DoubleType),
                            crosshairPoint.propertyValue("y", DoubleType) + adjustedCrosshairSize
                        ),
                        crosshairCol,
                        crosshairThickness
                    )

                    separate()

                    crosshair("clear")

                    separate()

                    foreach(variable(JvmOpenCvTypes.MatOfPoint, "contour"), inputPoints.value) {
                        // Get the bounding rectangle of the current contour
                        val boundingRect = uniqueVariable(
                            "boundingRect", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, it)
                        )
                        local(boundingRect)

                        separate()

                        // Check if the crosshair rectangle is inside the bounding rectangle
                        ifCondition(
                            boundingRect.callValue("contains", BooleanType, crosshairPoint).condition()
                        ) {
                            // Add the contour to the crosshair if the bounding rectangle contains the crosshair
                            crosshair("add", it)
                        }
                    }

                    drawCrosshairOn.streamIfEnabled(crosshairImage, drawOn.color)

                    session.outputCrosshair =
                        GenValue.GList.RuntimeListOf(crosshair, GenValue.GPoints.RuntimePoints::class)
                    session.outputCrosshairImage = GenValue.Mat(crosshairImage, drawOn.color, drawOn.isBinary)
                }
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when (attrib) {
            outputCrosshair -> current.sessionOf(this)!!.outputCrosshair
            outputCrosshairImage -> current.sessionOf(this)!!.outputCrosshairImage
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var outputCrosshair: GenValue.GList.RuntimeListOf<GenValue.GPoints.RuntimePoints>
        lateinit var outputCrosshairImage: GenValue.Mat
    }

}