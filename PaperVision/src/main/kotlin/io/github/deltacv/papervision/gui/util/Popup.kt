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
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.flags

class TooltipPopup(
    val text: String,
    override val position: ImVec2,
    val timeoutSeconds: Double,
    label: String
) : Popup(label) {

    companion object {
        fun warning(text: String) {
            for(popup in IdElementContainerStack.threadStack.peekNonNull<Popup>().inmutable) {
                if(popup is TooltipPopup && popup.label == "Warning") {
                    popup.delete()
                }
            }

            val position = ImGui.getMousePos()
            TooltipPopup(text, position, 6.0, "Warning").open()
        }
    }

    override val title = "Tooltip"
    override val flags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoScrollbar,
    )

    private val elapsedTime = ElapsedTime()

    override fun onEnable() {
        elapsedTime.reset()
    }

    override fun drawContents() {
        if(elapsedTime.seconds >= timeoutSeconds) {
            delete()
        }

        ImGui.text(tr(text))
    }

}

abstract class Popup(
    val label: String = ""
) : DrawableIdElementBase<Popup>() {
    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Popup>()

    abstract val title: String
    abstract val flags: Int

    private var open = false

    var isVisible = false
        private set

    open val position = ImGui.getMousePos()
    val idName by lazy { "${title}###$id" }

    private val pOpen = ImBoolean(false)

    abstract fun drawContents()

    override fun draw() {
        if(open) {
            ImGui.openPopup(idName)
            open = false
        }

        if(isVisible) {
            ImGui.setNextWindowPos(position)
        }

        if (ImGui.beginPopup(idName, flags)) {
            drawContents()
            ImGui.endPopup()
        }

        isVisible = ImGui.isPopupOpen(idName)
    }

    fun open() {
        if(!isEnabled) {
            enable()
        }

        open = true
    }
}