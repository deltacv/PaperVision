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
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Font
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.id.Misc
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

abstract class Window(
    override val requestedId: Int? = null,
) : DrawableIdElementBase<Window>() {

    override val idElementContainer = IdElementContainerStack.localStack.peekNonNull<Window>()

    var isVisible: Boolean = false
        private set

    abstract var title: String
    abstract val windowFlags: Int

    open val isModal: Boolean = false

    private var modalIsClosing = false

    private var nextPosition: ImVec2? = null
    private var internalPosition = ImVec2()

    val onDraw = PaperVisionEventHandler("Window-$title-OnDraw")

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
            if(firstDraw) {
                ImGui.closeCurrentPopup()
                ImGui.openPopup(titleId)
            }

            if(ImGui.beginPopupModal(titleId, modalPOpen, windowFlags)) {
                contents()

                if(modalIsClosing) {
                    ImGui.closeCurrentPopup()
                    super.delete() // bye bye finally
                }

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

        onDraw.run()

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

    override fun delete() {
        if(isModal && !modalIsClosing) {
            modalIsClosing = true
        } else {
            super.delete()
        }
    }
}

abstract class FrameWidthWindow : Window() {
    abstract var frameWidth: Float
        protected set
}

class DialogMessageWindow(
    override var title: String,
    val message: String,
    val textArea: String? = null,
    val font: Font? = null
): Window() {
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoScrollbar,
        ImGuiWindowFlags.NoScrollWithMouse
    )

    val textAreaId by Misc.newMiscId()

    override val isModal = true

    override fun drawContents() {
        font?.let {
            ImGui.pushFont(it.imfont)
        }

        val messageSize = ImGui.calcTextSize(tr(message))

        var maxWidth = messageSize.x
        var height = messageSize.y

        centeredText(message)

        ImGui.setCursorPos(ImVec2(ImGui.getCursorPosX(), ImGui.getCursorPosY() - 20))

        if(textArea != null) {
            val textSize = ImGui.calcTextSize(tr(textArea))
            ImGui.inputTextMultiline("##$textAreaId", ImString(textArea), ImGui.calcTextSize(tr(textArea)).x, textSize.y + 20)

            height += textSize.y.coerceAtMost(ImGui.getMainViewport().sizeY * 0.6f)
            maxWidth = maxOf(maxWidth, textSize.x)
        }

        ImGui.dummy(0f, 15f)
        ImGui.newLine()

        val gotItSize = ImGui.calcTextSize(tr("mis_gotit"))
        height += gotItSize.y * 2

        var buttonsWidth = gotItSize.x
        if(textArea != null) {
            buttonsWidth += ImGui.calcTextSize(tr("mis_copytext")).y
        }

        alignForWidth(buttonsWidth, 0.5f)

        if(ImGui.button(tr("mis_gotit"))) {
            delete()
        }

        if(textArea != null) {
            ImGui.sameLine()

            if(ImGui.button(tr("mis_copytext"))) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(
                    StringSelection(textArea), null
                )
            }
        }

        font?.let {
            ImGui.popFont()
        }

        size = ImVec2(maxWidth + 10, height + 80)
    }
}

fun centeredText(text: String) {
    val textSize = ImGui.calcTextSize(tr(text))
    val windowSize = ImGui.getWindowSize()
    val pos = windowSize.x / 2 - textSize.x / 2

    ImGui.sameLine(pos)
    ImGui.text(tr(text))
    ImGui.newLine()
}

fun alignForWidth(width: Float, alignment: Float): Float {
    val windowSize = ImGui.getWindowSize()
    val pos = windowSize.x / 2 - width / 2
    ImGui.sameLine(pos + alignment)

    return pos + alignment
}