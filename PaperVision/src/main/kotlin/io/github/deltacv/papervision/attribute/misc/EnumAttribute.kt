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
import imgui.type.ImInt
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.serialization.data.SerializeData

class EnumAttribute<T: Enum<T>>(
    override val mode: AttributeMode,
    val values: Array<T>,
    override var variableName: String?
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.FlagCheckered
        override val allowsNew = false

        override val styleColor = rgbaColor(46, 139, 87, 180)
        override val styleHoveredColor = rgbaColor(46, 139, 87, 255)
    }

    private val valuesStrings = values.map {
        it.name
    }.toTypedArray()

    @SerializeData
    val currentIndex = ImInt()

    val currentValue get() = values[currentIndex.get()]

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink) {
            ImGui.pushItemWidth(110.0f)
            ImGui.combo("", currentIndex, valuesStrings)
            ImGui.popItemWidth()
        }

        checkChange()
    }

    override fun acceptLink(other: Attribute) = other is EnumAttribute<*> && values[0]::class == other.values[0]::class

    override fun readEditorValue() = values[currentIndex.get()]

    @Suppress("UNCHECKED_CAST")
    override fun genValue(current: CodeGen.Current): GenValue.Enum<T> {
        val expectedClass = values[0]::class

        if(isInput) {
            if(hasLink) {
                val linkedAttrib = availableLinkedAttribute

                raiseAssert(
                    linkedAttrib != null,
                    "Enum attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.genValue(current)
                raiseAssert(value is GenValue.Enum<*>, "Attribute attached is not a valid Enum")

                val valueEnum = value as GenValue.Enum<*>

                raiseAssert(
                    value.clazz == expectedClass,
                    "Enum attribute attached (${value.clazz}) is not the expected type of enum ($expectedClass)"
                )

                return valueEnum as GenValue.Enum<T>
            } else {
                val value = values[currentIndex.get()]

                return GenValue.Enum(value, value::class.java)
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(value is GenValue.Enum<*>, "Value returned from the node is not an enum")

            val valueEnum = value as GenValue.Enum<T>
            raiseAssert(
                value.clazz == expectedClass,
                "Enum attribute returned from the node (${value.clazz}) is not the expected type of enum ($expectedClass)"
            )

            return valueEnum
        }
    }

}