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

package io.github.deltacv.papervision.attribute.math

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.serialization.AttributeSerializationData
import io.github.deltacv.papervision.util.Range2i

class IntAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    initialValue: Int = 0
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.Hashtag
        override fun new(mode: AttributeMode, variableName: String) = IntAttribute(mode, variableName)
    }

    val value = ImInt(initialValue)
    private val sliderValue = ImInt(initialValue)
    private var nextValue: Int? = null

    var disableInput = false
        set(value) {
            showAttributesCircles = !value
            field = value
        }

    private var range: Range2i = Range2i.DEFAULT_POSITIVE
    var isSlider = false
        private set

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            if(!isSlider || disableInput) {
                value.set(range.clip(value.get()))

                ImGui.inputInt("", value, 1, 100, if(disableInput) ImGuiInputTextFlags.ReadOnly else 0)
            } else {
                ImGui.sliderInt("", sliderValue.data, range.min, range.max)
                value.set(sliderValue.get())
            }

            checkChange()

            ImGui.popItemWidth()

            if(nextValue != null) {
                value.set(nextValue!!)
                sliderValue.set(nextValue!!)
                nextValue = null
            }
        }
    }

    fun sliderMode(range: Range2i) {
        this.range = range
        isSlider = true
    }

    fun fieldMode(range: Range2i = Range2i.DEFAULT_POSITIVE) {
        this.range = range
        isSlider = false
    }

    override fun readEditorValue() = value.get()

    override fun genValue(current: CodeGen.Current) = readGenValue(
        current, "an Int", GenValue.Int(value.get().resolved())
    ) { it is GenValue.Int }

    override fun makeSerializationData() = Data(value.get())

    override fun takeSerializationData(data: AttributeSerializationData) {
        if(data is Data) {
            nextValue = data.value
        }
    }

    data class Data(var value: Int = 0) : AttributeSerializationData()

}
