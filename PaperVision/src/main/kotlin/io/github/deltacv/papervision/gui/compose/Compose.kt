package io.github.deltacv.papervision.gui.compose

import imgui.ImVec2
import io.github.deltacv.papervision.gui.compose.item.ContainerItem
import io.github.deltacv.papervision.gui.compose.item.Item

class Compose(val availableSize: ImVec2) : ContainerItem {
    private val _children = mutableListOf<Item>()

    fun add(item: Item) {
        _children.add(item)
    }

    fun renderAll() {
        render()
        postRender()
    }

    override fun measure() = availableSize

    override val parent = this
    override val children get() = _children as List<Item>
}