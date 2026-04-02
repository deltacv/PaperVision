package io.github.deltacv.papervision.gui.compose.item.layout

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.papervision.gui.compose.item.Item

data class AlignedRowItem(
    val children: List<Item>,
    val alignment: Float,
    val spacing: Float,
    val width: Float
) : Item {

    override fun measure(): ImVec2 {
        var w = 0f
        var h = 0f

        for ((i, child) in children.withIndex()) {
            val size = child.measure()

            w += size.x
            if (i != children.lastIndex) {
                w += spacing
            }

            h = maxOf(h, size.y)
        }

        return ImVec2(w, h)
    }

    override fun render() {
        val size = measure()
        val start = ImGui.getCursorScreenPos()
        val offsetX = (width - size.x) * alignment

        // Move to the start of the aligned row
        ImGui.setCursorPosX(ImGui.getCursorPosX() + offsetX)

        for ((i, child) in children.withIndex()) {
            child.render()
            if (i < children.lastIndex) {
                ImGui.sameLine(0f, spacing) // Let ImGui handle the gap
            }
        }

        // Ensure the next UI element starts below this row
        ImGui.setCursorScreenPos(start.x, start.y + size.y)
    }
}