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

package io.github.deltacv.papervision.node.vision

import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.type.ImInt
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generatorFor
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.data.SerializeData

@PaperNode(
    name = "nod_pipelineinput",
    category = Category.FLOW,
    showInList = false
)
class InputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    override fun init() {
        editor.onDraw { remover ->
            if(serializedId != null) {
                remover.removeThis()
                return@onDraw
            }

            // stop the node from adjusting when the user pans
            remover.removeOn(editor.onEditorPan)

            windowSizeSupplier?.let {
                val nodeSize = ImVec2()
                ImNodes.getNodeDimensions(nodeSize, id)

                val windowSize = it()
                ImNodes.setNodeScreenSpacePos(id, nodeSize.x * 0.5f, windowSize.y / 2f - nodeSize.y / 2)

                // by default, the node editor starts with 4 nodes
                // InputMatNode, OutputMatNode, originNode, flagsNode
                // if there are more than 4 nodes, we'll stop adjusting the position
                // since it's likely the user is starting to work on their project
                if(editor.nodes.inmutable.size > 4 || ImNodes.isNodeSelected(id)) {
                    remover.removeThis()
                    editor.onEditorPan.run()
                }
            }
        }
    }

    @SerializeData
    val output = MatAttribute(OUTPUT, "$[att_input]")

    override fun onEnable() {
        + output.rebuildOnChange()
    }

    fun ensureAttributeExists() { // prevent weird oopsies due to the special way these persistent buddies are handled
        enable()
        output.enable()
    }

    override val generators = mutableMapOf(
        generatorFor(JavaLanguage) { NoSession }
    )

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute
    ) = when(current.language) {
        is CPythonLanguage -> GenValue.Mat(Variable(CPythonLanguage.NoType, "input"), ColorSpace.RGBA)
        else -> GenValue.Mat(Variable(JvmOpenCvTypes.Mat, "input"), ColorSpace.RGBA)
    }
}

@PaperNode(
    name = "nod_pipelineoutput",
    category = Category.FLOW,
    showInList = false
)
class OutputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    var streamId: Int? = null

    init {
        genOptions {
            genAtTheEnd = true
        }
    }

    override fun init() {
        editor.onDraw { remover ->
            if(serializedId != null) {
                remover.removeThis()
                return@onDraw
            }

            // stop the node from adjusting when the user pans
            remover.removeOn(editor.onEditorPan)

            windowSizeSupplier?.let {
                val nodeSize = ImVec2()
                ImNodes.getNodeDimensions(nodeSize, id)

                val windowSize = it()
                ImNodes.setNodeScreenSpacePos(id, windowSize.x - nodeSize.x * 1.5f, windowSize.y / 2f - nodeSize.y / 2f)

                // by default, the node editor starts with 4 nodes
                // InputMatNode, OutputMatNode, originNode, flagsNode
                // if there are more than 4 nodes, we'll stop adjusting the position
                // since it's likely the user is starting to work on their project
                if(editor.nodes.inmutable.size > 4 || ImNodes.isNodeSelected(id)) {
                    remover.removeThis()
                    editor.onEditorPan.run()
                }
            }
        }
    }

    val input = MatAttribute(INPUT, "$[att_output]")
    val crosshair = PointsAttribute(INPUT, "$[att_crosshair]")
    val exportedData = ListAttribute(INPUT, DoubleAttribute, "$[att_exporteddata]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + crosshair.rebuildOnChange()
        + exportedData.rebuildOnChange()
    }

    fun ensureAttributeExists() { // prevent weird oopsies due to the special way these persistent buddies are handled
        enable()
        input.enable()
        crosshair.enable()
        exportedData.enable()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val inputValue = input.value(current)

                current.scope {
                    if (crosshair.linkedAttributes().isNotEmpty()) {
                        val crosshairValue = crosshair.value(current)

                        ifCondition((crosshairValue.value notEqualsTo crosshairValue.value.type.nullVal)) {val boundingRect =
                            uniqueVariable("boundingRect", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, crosshairValue.value))
                            local(boundingRect)

                            separate()

                            // Calculate the centroid of the contour
                            val centroidX =
                                uniqueVariable("centroidX", (boundingRect.callValue("tl", JvmOpenCvTypes.Point).propertyValue("x", DoubleType) +
                                        boundingRect.callValue("br", JvmOpenCvTypes.Point)
                                            .propertyValue("x", DoubleType)) / 2.v)
                            val centroidY =
                                uniqueVariable("centroidY", (boundingRect.callValue("tl", JvmOpenCvTypes.Point).propertyValue("y", DoubleType) +
                                        boundingRect.callValue("br", JvmOpenCvTypes.Point)
                                            .propertyValue("y", DoubleType)) / 2.v)

                            local(centroidX)
                            local(centroidY)

                            separate()

                            val centroid = uniqueVariable("centroid", JvmOpenCvTypes.Point.new(centroidX, centroidY))
                            local(centroid)
                            val contourArea = uniqueVariable("contourArea", Imgproc.callValue("contourArea", DoubleType, crosshairValue.value))
                            local(contourArea)

                            separate()

                            val crosshairSize = 10.v
                            val crosshairThickness = 5.v

                            val crosshairCol = uniqueVariable("crosshairCol", JvmOpenCvTypes.Scalar.new(0.0.v, 255.0.v, 0.0.v))
                            local(crosshairCol)

                            // draw crosshair on the centroid

                            separate()

                            Imgproc("line",
                                inputValue.value,
                                JvmOpenCvTypes.Point.new(centroidX - crosshairSize, centroidY),
                                JvmOpenCvTypes.Point.new(centroidX + crosshairSize, centroidY),
                                crosshairCol,
                                crosshairThickness
                            )
                            Imgproc("line",
                                inputValue.value,
                                JvmOpenCvTypes.Point.new(centroidX, centroidY - crosshairSize),
                                JvmOpenCvTypes.Point.new(centroidX, centroidY + crosshairSize),
                                crosshairCol,
                                crosshairThickness
                            )
                        }

                        separate()
                    }

                    streamMat(streamId!!, inputValue.value, inputValue.color)
                    returnMethod(inputValue.value)

                    appendWhiteline = false
                }
            }

            NoSession
        }

        @Suppress("UNCHECKED_CAST")
        generatorFor(CPythonLanguage) {
            current {
                val inputValue = input.value(current)
                val crosshairValue = crosshair.value(current)
                val dataValue = exportedData.value(current)

                current.scope {
                    val llpython = uniqueVariable("llpython", if(dataValue is GenValue.GList.RuntimeListOf<*>) {
                        dataValue.value
                    } else if(dataValue is GenValue.GList.ListOf<*>) {
                        val data = mutableListOf<Value>()

                        for (d in dataValue.elements) {
                            if(d is GenValue.Double) {
                                data.add(d.value.v)
                            }
                        }

                        CPythonLanguage.newArrayOf(CPythonLanguage.NoType, *data.toTypedArray())
                    } else raise("Uh oh, this shouldn't happen"))

                    local(llpython)

                    separate()

                    returnMethod(CPythonLanguage.tuple(crosshairValue.value, inputValue.value, llpython))
                    appendWhiteline = false
                }

                NoSession
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}