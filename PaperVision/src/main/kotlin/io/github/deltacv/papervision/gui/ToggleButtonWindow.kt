/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.flags

open class ToggleButtonWindow(
    val textOff: String,
    val textOn: String,
    val buttonFont: Font? = null,
    initialState: Boolean = false
) : Window() {

    override var title = "toggle button"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    private var hovered = false
    private var lastPressed = false

    var isPressed = false
        private set

    var isToggled = initialState
        private set

    val onToggleOn by lazy { PaperEventHandler("ToggleButtonWindow-OnToggleOn") }
    val onToggleOff by lazy { PaperEventHandler("ToggleButtonWindow-OnToggleOff") }

    /**
     * Runs before window draw.
     * We only set background color here.
     */
    override fun preDrawContents() {
        val style = ImGui.getStyle()

        val color = when {
            isPressed -> style.getColor(ImGuiCol.ButtonActive)
            hovered -> style.getColor(ImGuiCol.ButtonHovered)
            else -> style.getColor(ImGuiCol.Button)
        }

        ImGui.pushStyleColor(ImGuiCol.WindowBg, color)
        // Remove window padding so button fills entire window
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
    }

    /**
     * Actual rendering + input handling
     */
    override fun drawContents() {
        // ---- interactive region ----
        ImGui.invisibleButton("##$id", size.x.coerceAtLeast(1f), size.y.coerceAtLeast(1f))

        hovered = ImGui.isItemHovered()
        val held = ImGui.isItemActive()

        // Check if button was released while hovered (proper button behavior)
        val released = lastPressed && !held && hovered

        isPressed = held

        if (released) {
            isToggled = !isToggled

            if (isToggled) {
                onToggleOn.run()
            } else {
                onToggleOff.run()
            }
        }
        lastPressed = held

        // ---- centered text ----
        val currentText = if (isToggled) textOn else textOff

        buttonFont?.imfont?.let { ImGui.pushFont(it) }

        val textSize = ImGui.calcTextSize(currentText)

        ImGui.setCursorPos(
            (size.x - textSize.x) / 2f,
            (size.y - textSize.y) / 2f
        )

        ImGui.text(currentText)

        buttonFont?.imfont?.let { ImGui.popFont() }

        // ---- restore style ----
        ImGui.popStyleVar() // WindowPadding
        ImGui.popStyleColor() // WindowBg
    }

    fun toggle() {
        isToggled = !isToggled

        if (isToggled) {
            onToggleOn.run()
        } else {
            onToggleOff.run()
        }
    }

    fun setToggled(toggled: Boolean) {
        if (isToggled != toggled) {
            toggle()
        }
    }
}