package io.github.deltacv.papervision.gui.compose.item

import imgui.ImVec2

interface Item {
    fun measure(): ImVec2
    fun render()

    fun postRender() { }
}