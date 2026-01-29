/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv
 *
 * GPLv3
 */

package io.github.deltacv.papervision.attribute.math

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImDouble
import imgui.type.ImFloat
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.id.Misc
import io.github.deltacv.papervision.serialization.AttributeSerializationData
import io.github.deltacv.papervision.util.Range2d

class DoubleAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.SquareRootAlt
        override fun new(mode: AttributeMode, variableName: String) =
            DoubleAttribute(mode, variableName)
    }

    val value = ImDouble()
    private val sliderValue = ImFloat()

    private val sliderId by Misc.newMiscId()

    private var nextValue: Double? = null

    var disableInput = false
        set(value) {
            showAttributesCircles = !value
            field = value
        }

    private var range = Range2d.DEFAULT_POSITIVE
    var isSlider = false
        private set

    override fun drawAttribute() {
        super.drawAttribute()

        if (!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()
            ImGui.pushItemWidth(110.0f)

            if (!isSlider || disableInput) {
                value.set(range.clip(value.get()))

                ImGui.inputDouble(
                    "",
                    value,
                    0.0,
                    0.0,
                    "%.6f",
                    if (disableInput) ImGuiInputTextFlags.ReadOnly else 0
                )
            } else {
                ImGui.sliderFloat(
                    "###$sliderId",
                    sliderValue.data,
                    range.min.toFloat(),
                    range.max.toFloat()
                )
                value.set(sliderValue.get().toDouble())
            }

            checkChange()
            ImGui.popItemWidth()

            if (nextValue != null) {
                value.set(nextValue!!)
                sliderValue.set(nextValue!!.toFloat())

                nextValue = null
            }
        }
    }

    fun sliderMode(range: Range2d) {
        this.range = range
        isSlider = true
    }

    fun fieldMode(range: Range2d = Range2d.DEFAULT_POSITIVE) {
        this.range = range
        isSlider = false
    }

    override fun readEditorValue() = value.get()

    override fun genValue(current: CodeGen.Current) = readGenValue(
        current,
        "a Double",
        GenValue.Double(value.get().resolved())
    ) { it is GenValue.Double }

    override fun makeSerializationData() = Data(value.get())

    override fun takeSerializationData(data: AttributeSerializationData) {
        if (data is Data) {
            nextValue = data.value
        }
    }

    data class Data(var value: Double = 0.0) : AttributeSerializationData()
}
