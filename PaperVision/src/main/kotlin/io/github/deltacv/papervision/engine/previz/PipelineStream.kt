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

package io.github.deltacv.papervision.engine.previz

import io.github.deltacv.papervision.engine.client.ByteMessageReceiver
import io.github.deltacv.papervision.engine.client.Handler
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.io.bytes
import io.github.deltacv.papervision.io.scaleToFit
import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.animation.TimedTextureAnimation
import io.github.deltacv.papervision.util.loggerForThis
import java.awt.image.BufferedImage

class PipelineStream(
    val sessionName: String,
    val byteReceiver: ByteMessageReceiver,
    val width: Int = 160,
    val height: Int = 120,
    val status: Status = Status.MINIMIZED,
    val offlineImages: Array<BufferedImage>? = null,
    val offlineImagesFps: Double = 1.0
) {

    enum class Status {
        MINIMIZED, MAXIMIZED
    }
    
    val logger by loggerForThis()

    var isStarted = false
        private set

    private var requestedMaximize = false
    private var requestedMinimize = false

    private val pendingTextureQIds = mutableListOf<Int>()
    private val texturesQIdMap = mutableMapOf<Int, PlatformTexture>()

    // get the texture container of the current thread
    val textures = IdElementContainerStack.threadStack.peekNonNull<PlatformTexture>()
    val textureQueue = IdElementContainerStack.threadStack.peekSingleNonNull<TextureProcessorQueue>()

    var offlineTexture: PlatformTexture? = null
        private set

    constructor(
        sessionName: String,
        engineClient: PaperVisionEngineClient,
        width: Int = 160,
        height: Int = 120,
        status: Status = Status.MINIMIZED,
        offlineImages: Array<BufferedImage>? = null,
        offlineImagesFps: Double = 1.0
    ) : this(sessionName, engineClient.byteReceiver, width, height, status, offlineImages, offlineImagesFps)

    init {
        initOfflineImages()
    }

    private fun initOfflineImages() {
        if(offlineImages != null && offlineImages.isNotEmpty()) {
            if(offlineImages.size == 1) {
                var img = offlineImages[0]

                if (img.width != width || img.height != height) {
                    img = img.scaleToFit(width, height)
                }

                offlineTexture = textureQueue.textureFactory.create(width, height, img.bytes(), ColorSpace.BGR)
            } else {
                val textures = mutableListOf<PlatformTexture>()

                for(image in offlineImages) {
                    var img = image

                    if (img.width != width || img.height != height) {
                        img = img.scaleToFit(width, height)
                    }

                    textures.add(textureQueue.textureFactory.create(width, height, img.bytes(), ColorSpace.RGB))
                }

                offlineTexture = TimedTextureAnimation(offlineImagesFps, textures.toTypedArray()).apply {
                    enable()
                }
            }
        }
    }

    private val defaultHandler: Handler = { id, tag, bytes ->
        if(tag == sessionName) {
            if(!texturesQIdMap.contains(id)) {
                if(textureQueue[id] == null) {
                    // we are waiting for this texture to be created
                    // if we have already queued it, we don't need to do it again
                    if(!pendingTextureQIds.contains(id)) {
                        // create a new texture if this is nowhere to be found
                        textureQueue.offerJpeg(id, width, height, bytes)
                        pendingTextureQIds.add(id)

                        logger.info("Creating new texture for qId $id")
                    }
                } else {
                    // save the texture in our own id map so that we can
                    // use setJpegAsync instead of going though the queue
                    texturesQIdMap[id] = textureQueue[id]!!
                    pendingTextureQIds.remove(id)
                    logger.info("Acknowledged texture for qId $id")
                }
            } else {
                // yay, we can process async now
                texturesQIdMap[id]!!.setJpegAsync(bytes)
            }
        }
    }

    fun start() {
        logger.info("Starting pipeline stream of $sessionName at {}x{}", width, height)

        byteReceiver.addHandler(sessionName, defaultHandler)

        isStarted = true
    }

    fun stop() {
        isStarted = false

        byteReceiver.removeHandler(defaultHandler)
        byteReceiver.stop()
    }

    fun isAtOfflineTexture(id: Int) = !isStarted

    fun textureOf(id: Int) = if(isStarted)
        texturesQIdMap[id] ?: offlineTexture
    else offlineTexture

    fun maximize() {
        requestedMaximize = true
    }

    fun minimize() {
        requestedMinimize = true
    }

    fun popRequestedMaximize() = requestedMaximize.also { requestedMaximize = false }
    fun popRequestedMinimize() = requestedMinimize.also { requestedMinimize = false }

}