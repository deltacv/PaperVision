package io.github.deltacv.easyvision.gui.util

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.mai18n.tr

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
    private var userFocus = false

    var focus: Boolean
        set(value) {
            userFocus = value
        }
        get() = realFocus

    override fun draw() {
        preDrawContents()

        if(nextPosition != null) {
            ImGui.setNextWindowPos(nextPosition!!.x, nextPosition!!.y)
        }
        if(nextSize != null) {
            ImGui.setNextWindowSize(nextSize!!.x, nextSize!!.y)
        }

        if(userFocus) {
            ImGui.setNextWindowFocus()
        }

        ImGui.begin("${tr(title)}###$id", windowFlags)
            drawContents()

            ImGui.getWindowPos(position)
            ImGui.getWindowSize(size)
            realFocus = ImGui.isWindowFocused()
        ImGui.end()
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