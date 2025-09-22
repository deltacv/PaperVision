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

import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImInt
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.util.ExtraWidgets
import io.github.deltacv.papervision.id.Misc
import io.github.deltacv.papervision.serialization.data.SerializeData
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler

class RangeAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    val valueMutator: (Int) -> Double = { it.toDouble() }
) : TypedAttribute(Companion) {
    companion object : AttributeType {
        override val icon = FontAwesomeIcons.TextWidth
        override fun new(mode: AttributeMode, variableName: String) = RangeAttribute(mode, variableName)
    }

    private var usesToggleChanged = false
    val onToggleChange by lazy {
        usesToggleChanged = true
        PaperVisionEventHandler("RangeAttribute-OnToggleChange")
    }

    var min = 0
        set(value) {
            field = value
            if(minValue.get() < value) {
                minValue.set(value)
            }
            if(maxValue.get() < value) {
                maxValue.set(value)
            }
        }
    var max = 255
        set(value) {
            field = value
            if(maxValue.get() > value) {
                maxValue.set(value)
            }
            if(minValue.get() > value) {
                minValue.set(value)
            }
        }

    var useToggle = false
    var useSliders = true

    @SerializeData
    val toggleValue = ImBoolean(true)

    @SerializeData
    val minValue = ImInt(min)
    @SerializeData
    val maxValue = ImInt(max)

    private var prevToggle: Boolean? = null
    private var prevMin: Int? = null
    private var prevMax: Int? = null

    private val toggleId by Misc.newMiscId()
    private val minId by Misc.newMiscId()
    private val maxId by Misc.newMiscId()

    override fun drawAttribute() {
        super.drawAttribute()

        if(useToggle && !toggleValue.get()) return

        if(!hasLink) {
            sameLineIfNeeded()

            if(useSliders) {
                ExtraWidgets.rangeSliders(
                    min, max,
                    minValue, maxValue,
                    minId, maxId,
                    width = 95f
                )
            } else {
                ExtraWidgets.rangeTextInputs(
                    min, max,
                    minValue, maxValue,
                    minId, maxId,
                    width = 95f
                )
            }

            val mn = minValue.get()
            val mx = maxValue.get()

            if(mn != prevMin || mx != prevMax) {
                changed()
            }

            prevMin = mn
            prevMax = mx
        }
    }

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT && !hasLink && useToggle) {
            ImGui.sameLine()

            ImGui.checkbox("###$toggleId", toggleValue)

            if(prevToggle == null || prevToggle != toggleValue.get()) {
                changed()

                if(usesToggleChanged) {
                    onToggleChange()
                }
            }
            prevToggle = toggleValue.get()
        }
    }

    override fun readEditorValue() = arrayOf(valueMutator(minValue.get()), valueMutator(maxValue.get()))

    override fun genValue(current: CodeGen.Current) = value(
        current, "a Range", GenValue.Range(
            GenValue.Double(valueMutator(minValue.get()).resolved()),
            GenValue.Double(valueMutator(maxValue.get()).resolved())
        )
    ) { it is GenValue.Range }

}


fun RangeAttribute.rebuildOnToggleChange() = apply {
    onToggleChange {
        if(idElementContainer[id] != null) {
            rebuildPreviz()
        }
    }
}