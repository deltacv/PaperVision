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

package org.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.AttributeMode
import org.deltacv.papervision.attribute.TypedAttribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.misc.ListAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.node.vision.ColorSpace
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.util.Range2d

@CodecType(instantiable = false)
class ScalarAttribute(
    mode: AttributeMode,
    color: ColorSpace,
    variableName: String? = null
) : ListAttribute<DoubleAttribute, GenValue.Double>(mode, variableName, DoubleAttribute, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    override var icon = FontAwesomeIcons.GripHorizontal

    private val defaultImGuiFont by Font.findLazy("default-20")

    override fun drawAttributeText(index: Int, attrib: Attribute): Boolean {
        if(index < color.channelNames.size) {
            val name = color.channelNames[index]
            val elementName = name + if(name.length == 1) " " else ""

            if(attrib is TypedAttribute<*>) {
                attrib.drawDescriptiveText = false
                attrib.inlineInput = true
            }

            ImGui.pushFont(defaultImGuiFont.imfont)
            ImGui.text(elementName)
            ImGui.popFont()

            return true
        }

        return false
    }

    override fun onElementCreation(element: Attribute) {
        if(element is DoubleAttribute) {
            element.roundValues = true
            element.sliderMode(Range2d(0.0, 255.0))
        }
    }

    override fun genValue(current: CodeGen.Current): GenValue.Scalar {
        val values = super.genValue(current).toActualOrNull()?.elements

        val value = GenValue.Scalar.Components(
            values?.getOrElse(0) { GenValue.Double.ZERO } ?: GenValue.Double.ZERO,
            values?.getOrElse(1) { GenValue.Double.ZERO } ?: GenValue.Double.ZERO,
            values?.getOrElse(2) { GenValue.Double.ZERO } ?: GenValue.Double.ZERO,
            values?.getOrElse(3) { GenValue.Double.ZERO } ?: GenValue.Double.ZERO,
        )

        return readGenValue(current, value)
    }

}



