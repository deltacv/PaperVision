package io.github.deltacv.easyvision.gui.util

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import imgui.internal.flag.ImGuiAxis
import imgui.type.ImFloat
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

    fun splitter(id: Int, splitVertically: Boolean, thickness: Float,
                 size1: ImFloat, size2: ImFloat,
                 minSize1: Float, minSize2: Float,
                 splitterLongAxisSize: Float = -1.0f): Boolean {

        val cursorPos = ImGui.getCursorPos()

        val minX = cursorPos.x + if(splitVertically) size1.get() else 0f
        val minY = cursorPos.y + if(splitVertically) 0f else size1.get()

        val maxX = minX + imgui.internal.ImGui.calcItemSizeX(
            if(splitVertically) thickness else splitterLongAxisSize,
            if(splitVertically) splitterLongAxisSize else thickness,
            0f, 0f
        )
        val maxY = minY + imgui.internal.ImGui.calcItemSizeY(
            if(splitVertically) splitterLongAxisSize else thickness,
            if(splitVertically) thickness else splitterLongAxisSize,
            0f, 0f
        )

        return imgui.internal.ImGui.splitterBehavior(
            minX, minY, maxX, maxY, id,
            if(splitVertically) ImGuiAxis.X else ImGuiAxis.Y,
            size1, size2, minSize1, minSize2
        )
    }

}

fun makeFont(size: Float): ImFont {
    val fontConfig = ImFontConfig()
    fontConfig.sizePixels = size
    fontConfig.oversampleH = 1
    fontConfig.oversampleV = 1
    fontConfig.pixelSnapH = false

    return ImGui.getIO().fonts.addFontDefault(fontConfig)
}