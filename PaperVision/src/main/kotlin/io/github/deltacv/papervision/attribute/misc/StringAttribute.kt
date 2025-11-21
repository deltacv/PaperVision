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

package io.github.deltacv.papervision.attribute.misc

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImString
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.serialization.AttributeSerializationData

class StringAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.EnvelopeOpenText

        override fun new(mode: AttributeMode, variableName: String) = StringAttribute(mode, variableName)
    }

    val value = ImString()

    private var nextValue: String? = null

    var disableInput = false
        set(value) {
            showAttributesCircles = !value
            field = value
        }

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            ImGui.inputText("", value, if(disableInput) ImGuiInputTextFlags.ReadOnly else 0)

            if(!ImGui.isItemFocused()) {
                checkChange()
            }

            ImGui.popItemWidth()

            if(nextValue != null) {
                value.set(nextValue!!)
                nextValue = null
            }
        }
    }

    override fun readEditorValue() = value.get()

    override fun genValue(current: CodeGen.Current) = readGenValue(
        current, "a String", GenValue.String(value.get().resolved())
    ) { it is GenValue.String }

    override fun makeSerializationData() = Data(value.get())

    override fun takeSerializationData(data: AttributeSerializationData) {
        if(data is Data) {
            nextValue = data.value
        }
    }

    data class Data(var value: String = "") : AttributeSerializationData()

}