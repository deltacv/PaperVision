package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.animation.TimedTextureAnimation
import io.github.deltacv.papervision.util.loggerForThis
import java.awt.image.BufferedImage
import java.lang.IllegalStateException

class PipelineStream(
    val paperVision: PaperVision,
    val width: Int = 160,
    val height: Int = 120,
    offlineImages: Array<BufferedImage>? = null,
    offlineImagesFps: Double = 1.0
) {

    companion object {
        const val opcode: Byte = 0xE
    }
    
    val logger by loggerForThis()

    var isStarting = false
        private set
    var isStarted = false
        private set

    val queue = TextureProcessorQueue(paperVision)

    var offlineTexture: PlatformTexture? = null
        private set

    init {
        if(offlineImages != null && offlineImages.isNotEmpty()) {
            if(offlineImages.size == 1) {
                var img = offlineImages[0]

                if (img.width != width || img.height != height) {
                    img = img.scaleToFit(width, height)
                }

                offlineTexture = paperVision.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR)
            } else {
                val textures = mutableListOf<PlatformTexture>()

                for(image in offlineImages) {
                    var img = image

                    if (img.width != width || img.height != height) {
                        img = img.scaleToFit(width, height)
                    }

                    textures.add(paperVision.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR))
                }

                offlineTexture = TimedTextureAnimation(offlineImagesFps, textures.toTypedArray()).apply {
                    enable()
                }
            }
        }
    }

    fun startIfNeeded() {
    }

    fun stop() {
    }

    fun textureOf(id: Int) = queue[id] ?: offlineTexture

    fun clear() = queue.clear()

}