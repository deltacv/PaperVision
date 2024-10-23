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
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.id.IdElementContainerStack

open class Tooltip(
    val text: String,
    val position: ImVec2,
    val timeSecs: Double,
    val label: String? = null,
    override val requestedId: Int? = null
) : DrawableIdElementBase<Tooltip>() {

    private val timer = ElapsedTime()

    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Tooltip>()

    override fun onEnable() {
        timer.reset()
    }

    override fun draw() {
        ImGui.setNextWindowPos(position.x, position.y)

        ImGui.beginTooltip()
            drawContents()
        ImGui.endTooltip()

        if(timer.seconds > timeSecs)
            delete()
    }

    open fun drawContents() {
        ImGui.text(tr(text))
    }

    companion object {
        val WARN = 0

        fun warning(text: String, secsPerCharacter: Double = 0.16) {
            Tooltip(text, ImGui.getMousePos(), text.length * secsPerCharacter, requestedId = WARN).enable()
        }
    }

}