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

package io.github.deltacv.papervision.attribute.math

import imgui.type.ImInt
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.util.ExtraWidgets
import io.github.deltacv.papervision.serialization.data.SerializeData

class RangeAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {
    companion object : AttributeType {
        override val icon = FontAwesomeIcons.TextWidth
        override fun new(mode: AttributeMode, variableName: String) = RangeAttribute(mode, variableName)
    }

    var min = 0
    var max = 255

    @SerializeData
    val minValue = ImInt(min)
    @SerializeData
    val maxValue = ImInt(max)

    private var prevMin: Int? = null
    private var prevMax: Int? = null

    private val minId by PaperVision.miscIds.nextId()
    private val maxId by PaperVision.miscIds.nextId()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink) {
            sameLineIfNeeded()

            ExtraWidgets.rangeSliders(
                min, max,
                minValue, maxValue,
                minId, maxId,
                width = 95f
            )

            val mn = minValue.get()
            val mx = maxValue.get()

            if(mn != prevMin || mx != prevMax) {
                changed()
            }

            prevMin = mn
            prevMax = mx
        }
    }

    override fun thisGet() = arrayOf(minValue.get().toDouble(), maxValue.get().toDouble())

    override fun value(current: CodeGen.Current) = value(
        current, "a Range", GenValue.Range(
            minValue.get().toDouble(),
            maxValue.get().toDouble()
        )
    ) { it is GenValue.Range }

}