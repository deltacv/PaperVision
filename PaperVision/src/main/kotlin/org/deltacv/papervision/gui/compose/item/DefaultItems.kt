package org.deltacv.papervision.gui.compose.item

import imgui.ImGui
import imgui.ImVec2
import org.deltacv.papervision.gui.compose.property.Property
import org.deltacv.papervision.gui.compose.property.type.Text
import org.deltacv.mai18n.tr

data class ButtonItem(
    override val parent: ContainerItem,
    val text: Property<Text>,
    val onClick: () -> Unit
) : Item {
    override fun measure(): ImVec2 {
        val t = text.get()

        t.font?.let { ImGui.pushFont(it.imfont) }
        val textSize = ImGui.calcTextSize(tr(t.string))
        t.font?.let { ImGui.popFont() }

        val pad = ImGui.getStyle().framePadding

        return ImVec2(
            textSize.x + pad.x * 2f,
            textSize.y + pad.y * 2f
        )
    }

    override fun render() {
        val t = text.get()

        t.font?.let { ImGui.pushFont(it.imfont) }

        if(ImGui.button(tr(t.string))) {
            onClick()
        }

        t.font?.let { ImGui.popFont() }
    }
}

data class TextItem(
    override val parent: ContainerItem,
    val text: Property<Text>
) : Item {
    override fun measure(): ImVec2 {
        val t = text.get()

        t.font?.let { ImGui.pushFont(it.imfont) }
        val size = ImVec2(
            ImGui.calcTextSize(tr(t.string)).x,
            ImGui.calcTextSize(tr(t.string)).y
        )
        t.font?.let { ImGui.popFont() }

        return size
    }

    override fun render() {
        val t = text.get()

        t.font?.let { ImGui.pushFont(it.imfont) }
        ImGui.text(tr(t.string))
        t.font?.let { ImGui.popFont() }
    }
}

class NewLineItem(override val parent: ContainerItem) : Item {
    override fun measure() = ImVec2(0f, ImGui.getStyle().itemSpacing.y)

    override fun render() {
        ImGui.newLine()
    }
}
