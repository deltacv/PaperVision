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

package io.github.deltacv.papervision.gui.display

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.engine.previz.ClientPrevizStream
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.util.flags

class ImageDisplayWindow(
    val imageDisplay: ImageDisplay
) : Window() {
    override var title = "Preview"

    override val windowFlags = flags(
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove
    )

    override fun drawContents() {
        imageDisplay.draw()

        val pipelineStream = imageDisplay.clientPrevizStream

        val buttonText = if(pipelineStream.sizing == ClientPrevizStream.Sizing.MINIMIZED) {
            "Maximize"
        } else {
            "Minimize"
        }

        if (ImGui.button(buttonText)) {
            if(pipelineStream.sizing == ClientPrevizStream.Sizing.MINIMIZED) {
                pipelineStream.maximize()
            } else {
                pipelineStream.minimize()
            }
        }

        ImGui.sameLine()

        val pipelineFps = pipelineStream.statistics.fps

        val statusText = if(pipelineStream.isAtOfflineTexture(imageDisplay.id))
            "mis_loading"
        else if(pipelineFps <= 0)
            "mis_runningok"
        else "mis_runningok_atfps"

        ImGui.text(tr(statusText, String.format("%.1f", pipelineFps)))
    }
}
