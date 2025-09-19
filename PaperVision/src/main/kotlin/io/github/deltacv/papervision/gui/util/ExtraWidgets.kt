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

package io.github.deltacv.papervision.gui.util

import imgui.ImGui
import imgui.flag.ImGuiMouseButton
import imgui.type.ImInt

object ExtraWidgets {

    fun rangeSliders(min: Int, max: Int,
                     minValue: ImInt, maxValue: ImInt,
                     minId: Int, maxId: Int,
                     width: Float = 110f) {
        ImGui.pushItemWidth(width)
        ImGui.sliderInt("###$minId", minValue.data, min, max)

        ImGui.sameLine()

        ImGui.sliderInt("###$maxId", maxValue.data, min, max)
        ImGui.popItemWidth()

        val mn = minValue.get()
        val mx = maxValue.get()

        if(mn > mx) {
            minValue.set(mx)
        }
        if(mx < mn) {
            maxValue.set(mn)
        }
    }
    fun rangeTextInputs(
        min: Int, max: Int,
        minValue: ImInt, maxValue: ImInt,
        minId: Int, maxId: Int,
        width: Float = 110f
    ) {
        ImGui.pushItemWidth(width)
        ImGui.inputInt("###$minId", minValue)
        ImGui.sameLine()
        ImGui.inputInt("###$maxId", maxValue)
        ImGui.popItemWidth()

        // Clamp values within range
        val mn = minValue.get().coerceIn(min, max)
        val mx = maxValue.get().coerceIn(min, max)

        if (mn > mx) {
            minValue.set(mx)
        } else {
            minValue.set(mn)
        }

        if (mx < mn) {
            maxValue.set(mn)
        } else {
            maxValue.set(mx)
        }
    }

    private val valuesStringCache = mutableMapOf<Class<*>, Array<String>>()

    fun <T: Enum<T>> enumCombo(values: Array<T>, currentItem: ImInt): T {
        val clazz = values[0]::class.java

        val valuesStrings = if (valuesStringCache.containsKey(clazz)) {
            valuesStringCache[clazz]!!
        } else {
            val v = values.map {
                it.name
            }.toTypedArray()
            valuesStringCache[clazz] = v

            v
        }

        ImGui.combo("", currentItem, valuesStrings)

        return values[currentItem.get()]
    }

    fun toggleButton(label: String, currentState: Boolean): Boolean {
        ImGui.button(label)

        return if(ImGui.isItemClicked(ImGuiMouseButton.Left)) {
            !currentState
        } else currentState
    }

}