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

package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.engine.previz.PipelineStream
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.util.flags

class ImageDisplayWindow(
    val imageDisplay: ImageDisplay
) : Window() {
    override var title = "Preview"

    override val windowFlags = flags(
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    override fun drawContents() {
        imageDisplay.drawStream()

        val pipelineStream = imageDisplay.pipelineStream

        val buttonText = if(pipelineStream.status == PipelineStream.Status.MINIMIZED) {
            "Maximize"
        } else {
            "Minimize"
        }

        if (ImGui.button(buttonText)) {
            if(pipelineStream.status == PipelineStream.Status.MINIMIZED) {
                pipelineStream.maximize()
            } else {
                pipelineStream.minimize()
            }
        }

        ImGui.sameLine()

        val statusText = if(pipelineStream.isAtOfflineTexture(imageDisplay.id))
            "mis_loading"
        else "mis_runningok"

        ImGui.text(tr(statusText))
    }
}