package io.github.deltacv.easyvision.gui.util

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer

abstract class Window : DrawableIdElement {

    abstract var title: String
    abstract val windowFlags: Int

    override val id by windows.nextId { this }

    private var nextPosition: ImVec2? = null

    var position = ImVec2()
        set(value) {
            nextPosition = value
            // dont assign the backing field lol
        }

    private var nextSize: ImVec2? = null

    var size = ImVec2()
        set(value) {
            nextSize = value
            // again dont assign the backing field lol
        }

    private var realFocus = false
    var focus = false // ignore warning
        get() = realFocus

    private var isFirstDraw = true

    override fun draw() {
        preDrawContents()

        if(nextPosition != null) {
            ImGui.setNextWindowPos(nextPosition!!.x, nextPosition!!.y)
        }
        if(nextSize != null) {
            ImGui.setNextWindowSize(nextSize!!.x, nextSize!!.y)
        }

        if(focus) {
            ImGui.setNextWindowFocus()
        }

        ImGui.begin("$title###$id", windowFlags)
            drawContents()

            //if(!isFirstDraw) {
                ImGui.getWindowPos(position)
                ImGui.getWindowSize(size)
                realFocus = ImGui.isWindowFocused()
            //}
        ImGui.end()

        isFirstDraw = false
    }

    open fun preDrawContents() { }

    abstract fun drawContents()

    override fun delete() {
        windows.removeId(id)
    }

    override fun restore() {
        windows[id] = this
    }

    companion object {
        val windows = IdElementContainer<Window>()
    }

}