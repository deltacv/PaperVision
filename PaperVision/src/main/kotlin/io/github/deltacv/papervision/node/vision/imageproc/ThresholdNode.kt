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

package io.github.deltacv.papervision.node.vision.imageproc

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarRangeAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Core
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Scalar
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.gui.util.ExtraWidgets
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.serialization.data.SerializeData

@PaperNode(
    name = "nod_colorthresh",
    category = Category.IMAGE_PROC,
    description = "des_colorthresh"
)
class ThresholdNode : DrawNode<ThresholdNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val scalar = ScalarRangeAttribute(INPUT, ColorSpace.values()[0], "$[att_threshold]")

    val output = MatAttribute(OUTPUT, "$[att_binaryoutput]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + scalar
        + output.enablePrevizButton().rebuildOnChange()
    }

    @SerializeData
    private var colorValue = ImInt()

    private var lastColor = ColorSpace.values()[0]

    override fun drawNode() {
        input.drawHere()

        ImGui.newLine()
        ImGui.text("(Enum) Color Space")

        ImGui.pushItemWidth(110.0f)
        val color = ExtraWidgets.enumCombo(ColorSpace.values(), colorValue)
        ImGui.popItemWidth()

        ImGui.newLine()

        if(color != lastColor) {
            scalar.color = color
            scalar.rebuildPreviz()
        }

        lastColor = color
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val range = scalar.value(current)

                var inputMat = input.value(current)
                inputMat.requireNonBinary(input)

                val matColor = inputMat.color
                val targetColor = lastColor

                val needsCvt = matColor != targetColor

                val cvtMat = uniqueVariable("${targetColor.name.lowercase()}Mat", Mat.new())
                val thresholdTargetMat = uniqueVariable("${targetColor.name.lowercase()}BinaryMat", Mat.new())

                val scalarLabels = scalar.labelsForTwoScalars()

                val lowerScalar = uniqueVariable("lower${targetColor.name}",
                    Scalar.new(
                        range.a.min.v,
                        range.b.min.v,
                        range.c.min.v,
                        range.d.min.v,
                    )
                )

                val upperScalar = uniqueVariable("upper${targetColor.name}",
                    Scalar.new(
                        range.a.max.v,
                        range.b.max.v,
                        range.c.max.v,
                        range.d.max.v,
                    )
                )

                group {
                    // lower color scalar
                    public(lowerScalar, scalarLabels.first)

                    // upper color scalar
                    public(upperScalar, scalarLabels.second)

                    if (needsCvt) {
                        private(cvtMat)
                    }
                    // output mat target
                    private(thresholdTargetMat)
                }

                current.scope {
                    if(needsCvt) {
                        Imgproc("cvtColor", inputMat.value, cvtMat, cvtColorValue(matColor, targetColor))
                        inputMat = GenValue.Mat(cvtMat, targetColor)
                    }

                    Core("inRange", inputMat.value, lowerScalar, upperScalar, thresholdTargetMat)
                    output.streamIfEnabled(thresholdTargetMat, ColorSpace.GRAY)
                }

                session.outputMat = GenValue.Mat(thresholdTargetMat, ColorSpace.GRAY, true)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val range = scalar.value(current)

                var inputMat = input.value(current)
                inputMat.requireNonBinary(input)

                val matColor = inputMat.color
                val targetColor = lastColor

                val needsCvt = matColor != targetColor

                val cvtMat = uniqueVariable(
                    targetColor.name.lowercase(),
                    cv2.callValue("cvtColor", CPythonLanguage.NoType, inputMat.value, cvtColorValue(matColor, targetColor))
                )

                val target = if(needsCvt) {
                    cvtMat
                } else inputMat.value

                val thresholdTargetMat = uniqueVariable("${targetColor.name.lowercase()}_thresh",
                    cv2.callValue("inRange", CPythonLanguage.NoType, target,
                        CPythonLanguage.tuple(range.a.min.v, range.b.min.v, range.c.min.v, range.d.min.v),
                        CPythonLanguage.tuple(range.a.max.v, range.b.max.v, range.c.max.v, range.d.max.v)
                    )
                )

                current.scope {
                    if(needsCvt) {
                        local(cvtMat)
                    }

                    local(thresholdTargetMat)

                    session.outputMat = GenValue.Mat(thresholdTargetMat, targetColor, true)
                }

                session
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return current.sessionOf(this)!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}