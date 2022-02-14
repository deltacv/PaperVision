package io.github.deltacv.easyvision.gui.util

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.DrawableIdElementBase
import io.github.deltacv.easyvision.id.IdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.mai18n.tr

abstract class Window(
    override val requestedId: Int? = null,
    override val idElementContainer: IdElementContainer<Window> = windows
) : DrawableIdElementBase<Window>() {

    abstract var title: String
    abstract val windowFlags: Int

    private var nextPosition: ImVec2? = null
    private var internalPosition = ImVec2()

    var position: ImVec2
        get() = internalPosition
        set(value) {
            nextPosition = value
            // dont assign the backing field lol
        }

    private var nextSize: ImVec2? = null
    private var internalSize = ImVec2()

    var size: ImVec2
        get() = internalSize
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

            ImGui.getWindowPos(internalPosition)
            ImGui.getWindowSize(internalSize)
            realFocus = ImGui.isWindowFocused()
        ImGui.end()
    }

    open fun preDrawContents() { }

    abstract fun drawContents()

    companion object {
        val windows = IdElementContainer<Window>()
    }

}

abstract class FrameWidthWindow : Window() {
    abstract var frameWidth: Float
        protected set
}