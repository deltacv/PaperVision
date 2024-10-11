package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
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
    }
}