package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
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

        if (ImGui.button("Maximize")) {
            imageDisplay.pipelineStream.maximize()
        }
    }
}