package io.github.deltacv.papervision.gui.compose

import imgui.ImVec2
import io.github.deltacv.papervision.gui.compose.item.Item

class Compose(val availableSize: ImVec2) {
    private val _items = mutableListOf<Item>()
    val items get() = _items as List<Item>

    fun add(item: Item) {
        _items.add(item)
    }

    fun renderAll() {
        for (node in _items) {
            node.render()
        }

        for(node in _items) {
            node.postRender()
        }
    }
}