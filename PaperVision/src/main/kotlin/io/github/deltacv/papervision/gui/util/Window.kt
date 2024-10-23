/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui.util

import imgui.ImGui
import imgui.ImVec2
import imgui.type.ImBoolean
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack

abstract class Window(
    override val requestedId: Int? = null,
) : DrawableIdElementBase<Window>() {

    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Window>()

    var isVisible: Boolean = false
        private set

    abstract var title: String
    abstract val windowFlags: Int

    open val isModal: Boolean = false

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

    private val modalPOpen = ImBoolean(true)

    var focus: Boolean
        set(value) {
            userFocus = value
        }
        get() = realFocus

    val titleId get() = "${tr(title)}###$id"

    private var firstDraw = true

    override fun enable() {
        super.enable()
        firstDraw = true
    }

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

        if(isModal) {
            if(firstDraw)
                ImGui.openPopup(titleId)

            if(ImGui.beginPopupModal(titleId, modalPOpen, windowFlags)) {
                contents()
                ImGui.endPopup()
            }

            isVisible = modalPOpen.get()
        } else {
            if(ImGui.begin(titleId, windowFlags)) {
                contents()
            }
            ImGui.end()

            isVisible = ImGui.isItemVisible()
        }

        firstDraw = false
    }

    private fun contents() {
        drawContents()

        ImGui.getWindowPos(internalPosition)
        ImGui.getWindowSize(internalSize)
        realFocus = ImGui.isWindowFocused()
    }

    open fun preDrawContents() { }

    abstract fun drawContents()

    fun centerWindow() {
        val displaySize = ImGui.getIO().displaySize
        position = ImVec2((displaySize.x - size.x) / 2, (displaySize.y - size.y) / 2)
    }

}

abstract class FrameWidthWindow : Window() {
    abstract var frameWidth: Float
        protected set
}