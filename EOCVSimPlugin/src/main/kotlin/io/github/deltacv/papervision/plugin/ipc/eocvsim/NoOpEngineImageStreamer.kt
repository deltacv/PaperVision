package io.github.deltacv.papervision.plugin.ipc.eocvsim

import io.github.deltacv.eocvsim.stream.ImageStreamer
import org.opencv.core.Mat

object NoOpEngineImageStreamer : ImageStreamer {
    override fun sendFrame(id: Int, image: Mat, cvtCode: Int?) {
        // no-op
    }
}