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
import io.github.deltacv.papervision.gui.editor.menu.SourceCodeExportSelectLanguageWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags

class SourceCodeExportButtonWindow(
    val floatingButtonSupplier: () -> NodeList.FloatingButton,
    val nodeEditorSizeSupplier: () -> ImVec2,
    val paperVision: PaperVision
) : Window() {
    override var title = ""
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    val fontAwesomeBig get() = Font.find("font-awesome-big")

    val frameWidth get() = floatingButtonSupplier().frameWidth

    var isPressed = false
        private set

    override fun preDrawContents() {
        position = ImVec2(
            floatingButtonSupplier().position.x - NodeList.Companion.PLUS_FONT_SIZE * 1.7f,
            floatingButtonSupplier().position.y,
        )
    }

    override fun drawContents() {
        ImGui.pushFont(fontAwesomeBig.imfont)

        isPressed = ImGui.button(
            FontAwesomeIcons.FileCode,
            floatingButtonSupplier().frameWidth,
            floatingButtonSupplier().frameWidth
        )

        if (isPressed) {
            SourceCodeExportSelectLanguageWindow(paperVision, nodeEditorSizeSupplier).enable()
        }

        ImGui.popFont()
    }
}
