package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.PipelineStream

class ImageDisplayWindow(
    val title: String,
    val stream: PipelineStream
) : DrawableIdElement {

    override val id by displayWindows.nextId(this)

    var currentPosition = ImVec2()
        set(value) {
            nextPosition = value
            // dont assign the backing field lol
        }

    private var nextPosition: ImVec2? = null

    override fun draw() {
        if(nextPosition != null) {
            ImGui.setNextWindowPos(nextPosition!!.x, nextPosition!!.y)
        }

        ImGui.begin(title)
            stream.textureOf(id)?.draw()
            ImGui.getWindowPos(currentPosition)
        ImGui.end()
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