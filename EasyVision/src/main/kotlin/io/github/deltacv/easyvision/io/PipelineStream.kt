package io.github.deltacv.easyvision.io

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.platform.ColorSpace
import io.github.deltacv.easyvision.platform.PlatformTexture
import io.github.deltacv.easyvision.platform.animation.TimedTextureAnimation
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.IsStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StartStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StopStreamingMessage
import java.awt.image.BufferedImage
import java.lang.IllegalStateException

class PipelineStream(
    val easyVision: EasyVision,
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

    val ipcClient get() = easyVision.eocvSimIpcClient

    val queue = TextureProcessorQueue(easyVision)

    var offlineTexture: PlatformTexture? = null
        private set

    init {
        ipcClient.onConnect {
            if(isStarted) {
                startIfNeeded()
            }
        }

        ipcClient.onDisconnect {
            isStarting = false
        }

        ipcClient.binaryHandler(opcode) { id, bytes ->
            if(isStarted) {
                queue.offer(id.toInt(), width, height, bytes)
            }
        }

        if(offlineImages != null && offlineImages.isNotEmpty()) {
            if(offlineImages.size == 1) {
                var img = offlineImages[0]

                if (img.width != width || img.height != height) {
                    img = img.scaleToFit(width, height)
                }

                offlineTexture = easyVision.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR)
            } else {
                val textures = mutableListOf<PlatformTexture>()

                for(image in offlineImages) {
                    var img = image

                    if (img.width != width || img.height != height) {
                        img = img.scaleToFit(width, height)
                    }

                    textures.add(easyVision.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR))
                }

                offlineTexture = TimedTextureAnimation(offlineImagesFps, textures.toTypedArray()).apply {
                    enable()
                }
            }
        }
    }

    fun startIfNeeded() {
        ipcClient.broadcast(IsStreamingMessage().onResponse { isStartedResponse ->
            if(isStartedResponse is IpcBooleanResponse) {
                isStarted = isStartedResponse.value

                if(!isStarted && !isStarting) {
                    isStarting = true

                    logger.info("Starting pipeline stream at ${width}x${height}")

                    ipcClient.broadcast(StartStreamingMessage(width, height, opcode).onResponse {
                        if (it is IpcOkResponse) {
                            isStarted = true
                            logger.info("Stream successfully started")
                        } else if (it is IpcErrorResponse && it.exception is IllegalStateException) {
                            isStarted = true
                            logger.info("Stream already started")
                        }
                        isStarting = false
                    })
                }
            }
        })
    }

    fun stop() {
        if(isStarted) {
            ipcClient.broadcast(StopStreamingMessage().onResponse {
                if(it is IpcOkResponse) {
                    isStarted = false
                }
            })
        }
    }

    fun textureOf(id: Int) = queue[id] ?: offlineTexture

    fun clear() = queue.clear()

}