package io.github.deltacv.easyvision.gui

import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.PipelineStream

class ImageDisplayWindow(
    override var title: String,
    val stream: PipelineStream
) : Window() {

    override val windowFlags = ImGuiWindowFlags.NoResize

    override val id by displayWindows.nextId(this)

    override fun drawContents() {
        stream.textureOf(id)?.draw()
    }

    override fun delete() {
        displayWindows.removeId(id)
    }

    override fun restore() {
        displayWindows[id] = this
    }

    companion object {
        val displayWindows = IdElementContainer<ImageDisplayWindow>()
    }

}