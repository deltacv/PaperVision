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
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.math.RangeAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.engine.client.message.TunerValue
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.util.hashCodeString

@CodecType(instantiable = false)
class ScalarRangeAttribute(
    mode: AttributeMode,
    color: ColorSpace,
    variableName: String? = null
) : ListAttribute<RangeAttribute, GenValue.Range>(mode, variableName, RangeAttribute, color.channels) {

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

    override fun genValue(current: CodeGen.Current): GenValue.ScalarRange {
        val values = super.genValue(current).toActualOrNull()!!.elements

        val range = GenValue.ScalarRange(
            values.getOrElse(0) { GenValue.Range.ZERO },
            values.getOrElse(1) { GenValue.Range.ZERO },
            values.getOrElse(2) { GenValue.Range.ZERO },
            values.getOrElse(3) { GenValue.Range.ZERO }
        )

        return readGenValue(current, range)
    }

    private var twoScalarsCached: Pair<String, String>? = null

    fun labelsForTwoScalars(): Pair<String, String> {
        if(twoScalarsCached != null) return twoScalarsCached!!

        val hexMin = hashCodeString
        val hexMax = hexMin.hashCodeString

        onChange {
            val values = editorValue?.let {
                it as Array<*>
            } ?: return@onChange

            val minValues = mutableListOf(0.0, 0.0, 0.0, 0.0)
            val maxValues = mutableListOf(0.0, 0.0, 0.0, 0.0)

            for((i, value) in values.withIndex()) {
                val valueArr = value as Array<*>

                minValues[i] = valueArr[0] as Double
                maxValues[i] = valueArr[1] as Double
            }

            broadcastLabelMessageFor(hexMin, TunerValue.ListValue(minValues.map { TunerValue.DoubleValue(it) }))
            broadcastLabelMessageFor(hexMax, TunerValue.ListValue(maxValues.map { TunerValue.DoubleValue(it) }))
        }

        twoScalarsCached = Pair(hexMin, hexMax)

        return twoScalarsCached!!
    }
}