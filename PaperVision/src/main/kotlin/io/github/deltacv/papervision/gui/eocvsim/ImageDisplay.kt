package io.github.deltacv.papervision.gui.eocvsim

import io.github.deltacv.papervision.id.IdElement
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.PipelineStream

class ImageDisplay(
    var pipelineStream: PipelineStream
) : IdElement {
    override val id by IdElementContainerStack.threadStack.peekNonNull<ImageDisplay>().nextId(this)

    fun drawStream() {
        pipelineStream.textureOf(id)?.draw()
    }
}