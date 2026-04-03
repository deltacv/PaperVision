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

package org.deltacv.papervision.gui.display

import imgui.ImGui
import org.deltacv.papervision.id.container.IdContainerStack
import org.deltacv.papervision.engine.previz.ClientPrevizStream
import org.deltacv.papervision.id.DrawableIdElementBase
import org.deltacv.papervision.util.ElapsedTime
import org.deltacv.mai18n.tr

class ImageDisplay(
    var clientPrevizStream: ClientPrevizStream
) : DrawableIdElementBase<ImageDisplay>() {

    override val idContainer get() = IdContainerStack.local.peekNonNull<ImageDisplay>()

    private val hoverTimer = ElapsedTime()

    override fun draw() {
        clientPrevizStream.textureOf(id)?.draw()

        if(ImGui.isItemHovered()) {
            if(hoverTimer.seconds >= 0.5) {
                 ImGui.setTooltip(if(clientPrevizStream.sizing == ClientPrevizStream.Sizing.MINIMIZED) {
                     tr("mis_doubleclick_tomaximize")
                 } else {
                     tr("mis_doubleclick_tominimize")
                 })
            }

            if(ImGui.isMouseDoubleClicked(0)) {
                if(clientPrevizStream.sizing == ClientPrevizStream.Sizing.MINIMIZED) {
                    clientPrevizStream.maximize()
                } else {
                    clientPrevizStream.minimize()
                }
            }
        } else {
            hoverTimer.reset()
        }
    }
}



