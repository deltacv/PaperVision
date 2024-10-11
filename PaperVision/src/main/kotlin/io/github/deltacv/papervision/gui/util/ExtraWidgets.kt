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