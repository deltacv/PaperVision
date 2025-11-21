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

package io.github.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.Range2i

class ScalarAttribute(
    mode: AttributeMode,
    color: ColorSpace,
    variableName: String? = null
) : ListAttribute(mode, IntAttribute, variableName, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    override var icon = FontAwesomeIcons.GripHorizontal

    private val defaultImGuiFont = Font.find("default-12")

    override fun drawAttributeText(index: Int, attrib: Attribute): Boolean {
        if(index < color.channelNames.size) {
            val name = color.channelNames[index]
            val elementName = name + if(name.length == 1) " " else ""

            if(attrib is TypedAttribute) {
                attrib.drawDescriptiveText = false
                attrib.inputSameLine = true
            }

            ImGui.pushFont(defaultImGuiFont.imfont)
            ImGui.text(elementName)
            ImGui.popFont()

            return true
        }

        return false
    }

    override fun onElementCreation(element: Attribute) {
        if(element is IntAttribute) {
            element.sliderMode(Range2i(0, 255))
        }
    }

    override fun genValue(current: CodeGen.Current): GenValue.Scalar {
        val values = (super.genValue(current) as GenValue.GList.List).elements

        val value = GenValue.Scalar(
            GenValue.Double((values.getOr(0, GenValue.Int.ZERO) as GenValue.Int).value.convertTo { it?.toDouble() }),
            GenValue.Double((values.getOr(1, GenValue.Int.ZERO) as GenValue.Int).value.convertTo { it?.toDouble() }),
            GenValue.Double((values.getOr(2, GenValue.Int.ZERO) as GenValue.Int).value.convertTo { it?.toDouble() }),
            GenValue.Double((values.getOr(3, GenValue.Int.ZERO) as GenValue.Int).value.convertTo { it?.toDouble() }),
        )

        return readGenValue(
            current, "a Scalar", value
        ) { it is GenValue.Scalar }
    }

}