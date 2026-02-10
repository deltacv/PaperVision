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

package io.github.deltacv.papervision.node.vision.imageproc

import imgui.ImGui
import imgui.type.ImInt
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarRangeAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolve.Resolvable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Core
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Scalar
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.gui.util.ImGuiEx
import io.github.deltacv.papervision.gui.util.Font
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

    private var lastColor = ColorSpace.entries.first()

    private val fontAwesome = Font.find("font-awesome")

    override fun drawNode() {
        input.drawHere()

        ImGui.newLine()

        ImGui.pushFont(fontAwesome.imfont)
        ImGui.text(EnumAttribute.icon)
        ImGui.popFont()

        ImGui.sameLine()
        ImGui.text(tr("att_colorspace"))

        ImGui.pushItemWidth(110.0f)
        val color = ImGuiEx.enumCombo(ColorSpace.values(), colorValue)
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

                val range = scalar.genValue(current)

                var inputMat = input.genValue(current)
                inputMat.requireNonBinary(input)

                val matColor = inputMat.color
                val targetColor = lastColor

                val cvtMat = uniqueVariable("${targetColor.name.lowercase()}Mat", Mat.new())
                val thresholdTargetMat = uniqueVariable("${targetColor.name.lowercase()}BinaryMat", Mat.new())

                val scalarLabels = scalar.labelsForTwoScalars()

                val lowerScalar = uniqueVariable("lower${targetColor.name}",
                    Scalar.new(
                        range.a.min.value.v,
                        range.b.min.value.v,
                        range.c.min.value.v,
                        range.d.min.value.v,
                    )
                )

                val upperScalar = uniqueVariable("upper${targetColor.name}",
                    Scalar.new(
                        range.a.max.value.v,
                        range.b.max.value.v,
                        range.c.max.value.v,
                        range.d.max.value.v,
                    )
                )

                group {
                    // lower color scalar
                    public(lowerScalar, scalarLabels.first)

                    // upper color scalar
                    public(upperScalar, scalarLabels.second)
                    // output mat target
                    private(thresholdTargetMat)
                }

                current.scope {
                    nameComment()

                    deferredBlock(Resolvable.DependentPlaceholder(matColor) {
                        {
                            if(it != targetColor) {
                                Imgproc("cvtColor", inputMat.value.v, thresholdTargetMat, cvtColorValue(it, targetColor))
                            } else {
                                inputMat.value.v("copyTo", thresholdTargetMat)
                            }
                        }
                    })

                    Core("inRange", thresholdTargetMat, lowerScalar, upperScalar, thresholdTargetMat)
                    output.streamIfEnabled(thresholdTargetMat, ColorSpace.GRAY.resolved())
                }

                session.outputMat = GenValue.Mat(thresholdTargetMat.resolved(), ColorSpace.GRAY.resolved(), GenValue.Boolean.TRUE)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val range = scalar.genValue(current)

                var inputMat = input.genValue(current)
                inputMat.requireNonBinary(input)

                val matColor = inputMat.color
                val targetColor = lastColor

                current.scope {
                    nameComment()

                    val target = uniqueVariable("thresholdTargetMat", inputMat.value.v)
                    local(target)

                    deferredBlock(Resolvable.DependentPlaceholder(matColor) {
                        {
                            target set cv2.callValue("cvtColor", CPythonLanguage.NoType, inputMat.value.v, cvtColorValue(it, targetColor))
                        }
                    })

                    val thresholdTargetMat = uniqueVariable("${targetColor.name.lowercase()}_thresh",
                        cv2.callValue("inRange", CPythonLanguage.NoType, target,
                            CPythonLanguage.tuple(range.a.min.value.v, range.b.min.value.v, range.c.min.value.v, range.d.min.value.v),
                            CPythonLanguage.tuple(range.a.max.value.v, range.b.max.value.v, range.c.max.value.v, range.d.max.value.v)
                        )
                    )

                    local(thresholdTargetMat)

                    session.outputMat = GenValue.Mat(thresholdTargetMat.resolved(), targetColor.resolved(), GenValue.Boolean.TRUE)
                }

                session
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMat }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}
