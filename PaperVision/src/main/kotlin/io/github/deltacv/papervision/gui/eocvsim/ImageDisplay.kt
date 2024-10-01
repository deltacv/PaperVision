package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import io.github.deltacv.papervision.id.IdElement
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.engine.previz.PipelineStream

class ImageDisplay(
    var pipelineStream: PipelineStream
) : IdElement {
    override val id by IdElementContainerStack.threadStack.peekNonNull<ImageDisplay>().nextId(this)

    fun drawStream() {
        pipelineStream.textureOf(id)?.draw()

        if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            pipelineStream.maximize()
        }
    }
}