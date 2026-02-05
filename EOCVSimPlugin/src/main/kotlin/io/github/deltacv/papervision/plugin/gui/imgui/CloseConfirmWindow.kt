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

package io.github.deltacv.papervision.plugin.gui.imgui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.util.flags

class CloseConfirmWindow(
    val callback: (Action) -> Unit
) : Window() {
    enum class Action {
        YES,
        NO,
        CANCEL
    }

    override var title = "win_confirm"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    override val modal = ModalMode.Modal(closeOnOutsideClick = false)

    override fun onEnable() {
        focus = true
    }

    override fun drawContents() {
        ImGui.text(tr("mis_savebefore_exit"))
        ImGui.separator()

        if(ImGui.button(tr("mis_save"))) {
            callback(Action.YES)
            delete()
        }
        ImGui.sameLine()
        if(ImGui.button(tr("mis_discard"))) {
            callback(Action.NO)
            delete()
        }
        ImGui.sameLine()
        if(ImGui.button(tr("mis_cancel"))) {
            callback(Action.CANCEL)
            delete()
        }
    }
}
