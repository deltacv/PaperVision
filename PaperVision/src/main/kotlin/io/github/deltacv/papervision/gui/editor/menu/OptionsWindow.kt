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

package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.Option
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.util.flags
import kotlin.collections.iterator

class OptionsWindow(
    val options: Map<String, Option>
) : Window() {
    override var title = "$[win_options]"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    val tooltipFont = Font.find("calcutta-big")
    val fontAwesomeBig = Font.find("font-awesome-big")

    override val modal = ModalMode.Modal()

    override fun drawContents() {
        ImGui.pushFont(fontAwesomeBig.imfont)
        ImGui.pushStyleColor(ImGuiCol.Button, 0)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0)

        for ((name, option) in options) {
            if (ImGui.button(name)) {
                option.action()
                delete()
            }

            if(ImGui.isItemHovered()) {
                ImGui.pushFont(tooltipFont.imfont)
                ImGui.setTooltip(tr(option.description))
                ImGui.popFont()
            }

            ImGui.sameLine()
            ImGui.indent(ImGui.getItemRectSizeX() * SourceCodeExportSelectLanguageWindow.Companion.SEPARATION_MULTIPLIER)
        }

        ImGui.popStyleColor()
        ImGui.popStyleColor()
        ImGui.popFont()
    }
}
