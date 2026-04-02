package io.github.deltacv.papervision.gui.compose.item

import imgui.ImGui
import imgui.ImVec2
import org.deltacv.mai18n.tr

sealed interface ComposeItem {
    fun measure(): ImVec2
    fun render()
}