package io.github.deltacv.papervision.gui.compose.item

import imgui.ImGui
import imgui.ImVec2
import org.deltacv.mai18n.tr

data class ButtonItem(
    val text: String,
    val onClick: () -> Unit
) : ComposeItem {
    override fun measure(): ImVec2 {
        val textSize = ImGui.calcTextSize(tr(text))
        val pad = ImGui.getStyle().framePadding

        return ImVec2(
            textSize.x + pad.x * 2f,
            textSize.y + pad.y * 2f
        )
    }

    override fun render() {
        if(ImGui.button(tr(text))) {
            onClick()
        }
    }
}

data class TextItem(
    val text: String
) : ComposeItem {
    override fun measure() = ImVec2(
        ImGui.calcTextSize(tr(text)).x,
        ImGui.calcTextSize(tr(text)).y
    )

    override fun render() {
        ImGui.text(tr(text))
    }
}

class NewLineItem : ComposeItem {
    override fun measure() = ImVec2(0f, ImGui.getStyle().itemSpacing.y)

    override fun render() {
        ImGui.newLine()
    }
}