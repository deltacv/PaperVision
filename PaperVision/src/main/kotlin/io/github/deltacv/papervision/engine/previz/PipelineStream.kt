package io.github.deltacv.papervision.engine.previz

import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.io.bytes
import io.github.deltacv.papervision.io.scaleToFit
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.animation.TimedTextureAnimation
import io.github.deltacv.papervision.util.loggerForThis
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class PipelineStream(
    val sessionName: String,
    val engineClient: PaperVisionEngineClient,
    val queue: TextureProcessorQueue,
    val width: Int = 160,
    val height: Int = 120,
    val status: Status = Status.MINIMIZED,
    offlineImages: Array<BufferedImage>? = null,
    offlineImagesFps: Double = 1.0
) {

    enum class Status {
        MINIMIZED, MAXIMIZED
    }
    
    val logger by loggerForThis()

    var isStarted = false
        private set

    private var requestedMaximize = false

    private var requestedMinimize = false

    val tag = ByteMessageTag.fromString(sessionName)

    var offlineTexture: PlatformTexture? = null
        private set

    init {
        if(offlineImages != null && offlineImages.isNotEmpty()) {
            if(offlineImages.size == 1) {
                var img = offlineImages[0]

                if (img.width != width || img.height != height) {
                    img = img.scaleToFit(width, height)
                }

                offlineTexture = queue.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR)
            } else {
                val textures = mutableListOf<PlatformTexture>()

                for(image in offlineImages) {
                    var img = image

                    if (img.width != width || img.height != height) {
                        img = img.scaleToFit(width, height)
                    }

                    textures.add(queue.textureFactory.create(width, height, img.bytes(), ColorSpace.RGB))
                }

                offlineTexture = TimedTextureAnimation(offlineImagesFps, textures.toTypedArray()).apply {
                    enable()
                }
            }
        }
    }

    fun start() {
        logger.info("Starting pipeline stream of $sessionName at {}x{}", width, height)

        engineClient.setByteMessageHandlerOf(tag) {
            val id = ByteMessages.idFromBytes(it)
            val message = ByteMessages.messageFromBytes(it)

            queue.offer(id, width, height, ByteBuffer.wrap(message))
        }

        isStarted = true
    }

    fun stop() {
        isStarted = false
        engineClient.clearByteMessageHandlerOf(tag)
        clear()
    }

    fun textureOf(id: Int) = if(isStarted)
        queue[id] ?: offlineTexture
    else offlineTexture

    fun clear() = queue.clear()

    fun maximize() {
        requestedMaximize = true
    }

    fun minimize() {
        requestedMinimize = true
    }

    fun popRequestedMaximize() = requestedMaximize.also { requestedMaximize = false }
    fun popRequestedMinimize() = requestedMinimize.also { requestedMinimize = false }

}