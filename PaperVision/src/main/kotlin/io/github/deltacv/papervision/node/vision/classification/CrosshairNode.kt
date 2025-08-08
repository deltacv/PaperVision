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
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
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

    enum class DetectionMode {
        Inside, Nearest
    }

    val drawCrosshairOn = MatAttribute(INPUT, "$[att_drawon_image]")
    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val crosshairPosition = Vector2Attribute(INPUT, "$[att_crosshairposition]")

    val crosshairScale = IntAttribute(INPUT, "$[att_scale]")

    val crosshairLineParams = LineParametersAttribute(INPUT, "$[att_crosshairline_params]")

    val detectionMode = EnumAttribute(INPUT, DetectionMode.values(), "$[att_detectionmode]")

    val outputCrosshair = ListAttribute(OUTPUT, PointsAttribute, "$[att_crosshair]")
    val outputCrosshairImage = MatAttribute(OUTPUT, "$[att_crosshairimage]")

    override fun onEnable() {
        + drawCrosshairOn.rebuildOnChange()
        + input.rebuildOnChange()
        + crosshairScale

        crosshairScale.value.set(5)

        + crosshairPosition
        + crosshairLineParams

        + detectionMode.rebuildOnChange()

        + outputCrosshairImage.enablePrevizButton().rebuildOnChange()
        + outputCrosshair.rebuildOnChange()
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
                    writeNameComment()

                    drawOnValue("copyTo", crosshairImage)

                    separate()

                    val crosshairPositionVector = crosshairPosition.value(current).ensureRuntimeVector2Java(current)

                    val crosshairPoint = uniqueVariable(
                        "crosshairPoint", run {
                            // draw crosshair at center with vector offset
                            val rows = drawOnValue.callValue("rows", IntType)
                            val cols = drawOnValue.callValue("cols", IntType)

                            JvmOpenCvTypes.Point.new((double(cols) / 2.v) + crosshairPositionVector.xValue, (double(rows) / 2.v) + crosshairPositionVector.yValue)
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

                    val currDist = if(detectionMode.value(current).value == DetectionMode.Nearest) {
                        uniqueVariable("currDist", 0.0.v)
                    } else {
                        null
                    }

                    val closestContour = if(detectionMode.value(current).value == DetectionMode.Nearest) {
                        uniqueVariable("closestContour", JvmOpenCvTypes.MatOfPoint.nullVal)
                    } else {
                        null
                    }

                    if(currDist != null) {
                        local(currDist)
                    }
                    if(closestContour != null) {
                        local(closestContour)
                        separate()
                    }

                    foreach(variable(JvmOpenCvTypes.MatOfPoint, "contour"), inputPoints.value) {
                        // Get the bounding rectangle of the current contour
                        val boundingRect = uniqueVariable(
                            "boundingRect", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, it)
                        )
                        local(boundingRect)

                        separate()

                        when(detectionMode.value(current).value) {
                            DetectionMode.Inside -> {
                                // Check if the crosshair rectangle is inside the bounding rectangle
                                ifCondition(
                                    boundingRect.callValue("contains", BooleanType, crosshairPoint).condition()
                                ) {
                                    // Add the contour to the crosshair if the bounding rectangle contains the crosshair
                                    crosshair("add", it)
                                }
                            }
                            DetectionMode.Nearest -> {
                                // get distance
                                val distance = uniqueVariable("newDist", JavaTypes.Math.callValue("sqrt", DoubleType,
                                    JavaTypes.Math.callValue("pow", DoubleType,
                                        crosshairPoint.propertyValue("x", DoubleType) - (boundingRect.propertyValue("x", IntType) + boundingRect.propertyValue("width", IntType) / 2.v),
                                        2.v
                                    ) + JavaTypes.Math.callValue("pow", DoubleType,
                                        crosshairPoint.propertyValue("y", DoubleType) - (boundingRect.propertyValue("y", IntType) + boundingRect.propertyValue("height", IntType) / 2.v),
                                        2.v
                                    )
                                ))

                                local(distance)

                                ifCondition((closestContour!! equalsTo JvmOpenCvTypes.MatOfPoint.nullVal) or (JavaTypes.Math.callValue("abs", DoubleType, distance) lessOrEqualThan currDist!!)) {
                                    currDist set JavaTypes.Math.callValue("abs", DoubleType, distance)
                                    closestContour set it
                                }
                            }
                        }
                    }

                    if(DetectionMode.Nearest == detectionMode.value(current).value) {
                        ifCondition(closestContour!! notEqualsTo JvmOpenCvTypes.MatOfPoint.nullVal) {
                            crosshair("add", closestContour)
                        }
                    }

                    outputCrosshairImage.streamIfEnabled(crosshairImage, drawOn.color)

                    session.outputCrosshair = GenValue.GList.RuntimeListOf(crosshair, GenValue.GPoints.RuntimePoints::class)
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


            val crosshairPositionVector = crosshairPosition.value(current)
            if(crosshairPositionVector !is GenValue.Vec2.Vector2) {
                raise("Crosshair position must not be runtime")
            }

            val drawOn = drawCrosshairOn.value(current)
            val drawOnValue = drawOn.value

            val crosshairLineParams = (crosshairLineParams.value(current) as GenValue.LineParameters.Line)
            val crosshairSizeValue = crosshairScale.value(current).value

            current {
                val crosshair = uniqueVariable("crosshair", CPythonLanguage.NoType.newArray())
                val crosshairImage = uniqueVariable("crosshair_image", drawOnValue.callValue("copy", CPythonLanguage.NoType))

                current.scope {
                    writeNameComment()

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

                    val (crosshairPointX, crosshairPointY) = Pair((cols / 2.v) + crosshairPositionVector.x.v, (rows / 2.v) + crosshairPositionVector.y.v)

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
                            CPythonOpenCvTypes.cv2.callValue("boundingRect", CPythonLanguage.NoType, it),
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
            outputCrosshair -> current.nonNullSessionOf(this).outputCrosshair
            outputCrosshairImage -> current.nonNullSessionOf(this).outputCrosshairImage
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var outputCrosshair: GenValue.GList.RuntimeListOf<GenValue.GPoints.RuntimePoints>
        lateinit var outputCrosshairImage: GenValue.Mat
    }

}