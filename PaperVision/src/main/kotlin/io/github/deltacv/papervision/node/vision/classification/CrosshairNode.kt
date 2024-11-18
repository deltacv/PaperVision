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
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import jdk.nashorn.internal.codegen.types.BooleanType

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
        +drawCrosshairOn.rebuildOnChange()
        +input.rebuildOnChange()
        +crosshairScale

        crosshairScale.value.set(5)

        +crosshairPosition
        +crosshairLineParams

        crosshairPosition.onChange {
            when (crosshairPosition.get()) {
                CrosshairPosition.Center -> {
                }

                CrosshairPosition.Offset -> {
                }

                CrosshairPosition.Custom -> {
                }
            }
        }

        +outputCrosshairImage.enablePrevizButton().rebuildOnChange()
        +outputCrosshair.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val inputPoints = input.value(current)

            if (inputPoints !is GenValue.GList.RuntimeListOf<*>) {
                raise("") // TODO: Handle non-runtime lists
            }

            val drawOn = drawCrosshairOn.value(current)
            val drawOnValue = drawOn.value

            val crosshairPosValue = crosshairPosition.value(current).value

            val crosshairLineParams = (crosshairLineParams.value(current) as GenValue.LineParameters).ensureRuntimeLineJava(current)

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

                    outputCrosshairImage.streamIfEnabled(crosshairImage, drawOn.color)

                    session.outputCrosshair =
                        GenValue.GList.RuntimeListOf(crosshair, GenValue.GPoints.RuntimePoints::class)
                    session.outputCrosshairImage = GenValue.Mat(crosshairImage, drawOn.color, drawOn.isBinary)
                }
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val inputPoints = input.value(current)

            if (inputPoints !is GenValue.GList.RuntimeListOf<*>) {
                raise("") // TODO: Handle non-runtime lists
            }

            val lineParams = crosshairLineParams.value(current)
            if (lineParams !is GenValue.LineParameters.Line) {
                raise("Line parameters must not be runtime")
            }

            val drawOn = drawCrosshairOn.value(current)
            val drawOnValue = drawOn.value

            val crosshairPosValue = crosshairPosition.value(current).value
            val crosshairLineParams = (crosshairLineParams.value(current) as GenValue.LineParameters.Line)
            val crosshairSizeValue = crosshairScale.value(current).value

            current {
                val crosshair = uniqueVariable("crosshair", CPythonLanguage.NoType.newArray())
                val crosshairImage = uniqueVariable("crosshair_image", drawOnValue.callValue("copy", CPythonLanguage.NoType))

                current.scope {
                    local(crosshair)
                    local(crosshairImage)

                    separate()

                    val rowsCols = CPythonLanguage.tupleVariables(
                        crosshairImage.propertyValue("shape", CPythonLanguage.NoType),
                        "height", "width", "channels"
                    )

                    local(rowsCols)

                    val rows = rowsCols.get("height")
                    val cols = rowsCols.get("width")

                    separate()

                    val (crosshairPointX, crosshairPointY) = if (crosshairPosValue == CrosshairPosition.Center) {
                        // draw crosshair at center
                        Pair(cols / 2.v, rows / 2.v)
                    } else if (crosshairPosValue == CrosshairPosition.Offset) {
                        TODO()
                    } else {
                        TODO()
                    }

                    val pointX = uniqueVariable("crosshair_point_x", crosshairPointX)
                    val pointY = uniqueVariable("crosshair_point_y", crosshairPointY)

                    local(pointX)
                    local(pointY)

                    // Define a scale factor based on the image dimensions
                    val scaleFactor = uniqueVariable("scale_factor", (rows + cols) / 2.v)
                    local(scaleFactor)

                    separate()

                    // Adjust crosshairSize based on the scale factor
                    val adjustedCrosshairSize =
                        uniqueVariable("adjusted_crosshair_size", crosshairSizeValue.v * scaleFactor / 100.v)
                    local(adjustedCrosshairSize)

                    separate()

                    val crosshairCol = crosshairLineParams.color.elements.map { it.value.v }.toTypedArray()
                    val crosshairThickness = crosshairLineParams.thickness.value.v

                    CPythonOpenCvTypes.cv2(
                        "line",
                        crosshairImage,
                        CPythonLanguage.tuple(
                            int(pointX - adjustedCrosshairSize),
                            int(pointY)
                        ),
                        CPythonLanguage.tuple(
                            int(pointX + adjustedCrosshairSize),
                            int(pointY)
                        ),
                        CPythonLanguage.tuple(*crosshairCol),
                        crosshairThickness
                    )

                    CPythonOpenCvTypes.cv2(
                        "line",
                        crosshairImage,
                        CPythonLanguage.tuple(
                            int(pointX),
                            int(pointY - adjustedCrosshairSize)
                        ),
                        CPythonLanguage.tuple(
                            int(pointX),
                            int(pointY + adjustedCrosshairSize)
                        ),
                        CPythonLanguage.tuple(*crosshairCol),
                        crosshairThickness
                    )

                    separate()

                    foreach(variable(CPythonLanguage.NoType, "contour"), inputPoints.value) {
                        // Get the bounding rectangle of the current contour
                        val boundingRect = CPythonLanguage.tupleVariables(
                            CPythonOpenCvTypes.cv2.callValue("bounding_rect", CPythonLanguage.NoType, it),
                            "x", "y", "w", "h"
                        )
                        local(boundingRect)

                        separate()
                        // Extract x, y, w, h values
                        val x = boundingRect.get("x")
                        val y = boundingRect.get("y")
                        val w = boundingRect.get("w")
                        val h = boundingRect.get("h")

                        // Check if the crosshair rectangle is inside the bounding rectangle
                        // perform aabb check
                        ifCondition(
                            (pointX greaterOrEqualThan x)
                                    and (pointX lessOrEqualThan (x + w))
                                    and (pointY greaterOrEqualThan y)
                                    and (pointY lessOrEqualThan (y + h))
                        ) {
                            // Add the contour to the crosshair if the bounding rectangle contains the crosshair
                            crosshair("append", it)
                        }
                    }

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