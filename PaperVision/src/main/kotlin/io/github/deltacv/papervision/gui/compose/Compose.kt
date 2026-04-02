package io.github.deltacv.papervision.gui.compose

import imgui.ImVec2
import io.github.deltacv.papervision.gui.compose.item.ComposeItem

class Compose(val availableSize: ImVec2) {
    private val _items = mutableListOf<ComposeItem>()
    val items get() = _items as List<ComposeItem>

    fun add(item: ComposeItem) {
        _items.add(item)
    }

    fun renderAll() {
        for (node in _items) {
            node.render()
        }
    }
}