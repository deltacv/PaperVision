package io.github.deltacv.easyvision.gui.eocvsim

import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.PipelineStream

class ImageDisplayWindow(
    override var title: String,
    val stream: PipelineStream
) : Window() {

    override val windowFlags = ImGuiWindowFlags.NoResize

    val displayId by displayWindows.nextId(this)

    override fun drawContents() {
        stream.textureOf(displayId)?.draw()
    }

    override fun delete() {
        super.delete()
        displayWindows.removeId(displayId)
    }

    override fun restore() {
        super.restore()
        displayWindows[displayId] = this
    }

    companion object {
        val displayWindows = IdElementContainer<ImageDisplayWindow>()
    }

}