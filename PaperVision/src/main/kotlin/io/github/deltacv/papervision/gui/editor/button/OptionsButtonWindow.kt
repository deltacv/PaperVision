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

package io.github.deltacv.papervision.gui.editor.button

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.gui.editor.Option
import io.github.deltacv.papervision.gui.editor.menu.OptionsWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags

class OptionsButtonWindow(
    val eocvSimPlayButtonWindow: PlayButtonWindow,
    val paperVision: PaperVision,
    val options: Map<String, Option>,
) : Window() {


    val tooltipFont = Font.find("calcutta-big")
    val fontAwesome = Font.find("font-awesome-big")

    override var title = "options control"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    private var lastButton = false

    var isPressed = false
        private set

    override fun preDrawContents() {
        val floatingButton = eocvSimPlayButtonWindow

        position = ImVec2(
            floatingButton.position.x - NodeList.Companion.PLUS_FONT_SIZE * 1.7f,
            floatingButton.position.y,
        )
    }

    override fun drawContents() {
        val floatingButton = eocvSimPlayButtonWindow

        ImGui.pushFont(fontAwesome.imfont)

        val text = FontAwesomeIcons.Gear

        isPressed = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

        if (lastButton != isPressed && isPressed) {
            OptionsWindow(options).enable()
        }

        ImGui.popFont()

        lastButton = isPressed
    }
}
