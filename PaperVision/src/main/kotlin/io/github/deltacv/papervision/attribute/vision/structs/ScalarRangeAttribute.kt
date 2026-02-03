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

package io.github.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.math.RangeAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.hexString

class ScalarRangeAttribute(
    mode: AttributeMode,
    color: ColorSpace,
    variableName: String? = null
) : ListAttribute(mode, RangeAttribute, variableName, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    private val defaultImGuiFont by Font.findLazy("default-20")

    override var icon = FontAwesomeIcons.GripVertical

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

    override fun genValue(current: CodeGen.Current): GenValue.ScalarRange {
        val values = (super.genValue(current) as GenValue.GList.List).elements
        val ZERO = GenValue.Range.ZERO

        val range = GenValue.ScalarRange(
            values.getOr(0, ZERO) as GenValue.Range,
            values.getOr(1, ZERO) as GenValue.Range,
            values.getOr(2, ZERO) as GenValue.Range,
            values.getOr(3, ZERO) as GenValue.Range
        )

        return readGenValue(current, "a scalar range", range) {
            it is GenValue.ScalarRange
        }
    }

    private var twoScalarsCached: Pair<String, String>? = null

    fun labelsForTwoScalars(): Pair<String, String> {
        if(twoScalarsCached != null) return twoScalarsCached!!

        val hexMin = hexString
        val hexMax = hexMin.hexString

        onChange {
            val values = editorValue?.let {
                it as Array<*>
            } ?: return@onChange

            val minValues = arrayOf(0.0, 0.0, 0.0, 0.0)
            val maxValues = arrayOf(0.0, 0.0, 0.0, 0.0)

            for((i, value) in values.withIndex()) {
                val valueArr = value as Array<*>

                minValues[i] = valueArr[0] as Double
                maxValues[i] = valueArr[1] as Double
            }

            broadcastLabelMessageFor(hexMin, minValues)
            broadcastLabelMessageFor(hexMax, maxValues)
        }

        twoScalarsCached = Pair(hexMin, hexMax)

        return twoScalarsCached!!
    }

}

fun <T> Array<T>.getOr(index: Int, or: T) = try {
    this[index]
} catch(ignored: ArrayIndexOutOfBoundsException) {
    or
}
