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

package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.platform.ColorSpace
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) {

    companion object {
        const val REUSABLE_ARRAY_QUEUE_SIZE = 4
    }

    private val reusableArrays = mutableMapOf<Int, ArrayBlockingQueue<WeakReference<ByteArray>>>()

    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(5)
    private val textures = mutableMapOf<Int, PlatformTexture>()

    private var currentHandler: PaperVisionEventHandler? = null

    fun subscribeTo(handler: PaperVisionEventHandler) {
        handler {
            if(currentHandler == handler) { // can only subscribe to one event loop at a time
                it.removeThis()
                return@handler
            }

            while(queuedTextures.isNotEmpty()) {
                val futureTex = queuedTextures.poll()

                if(textures.contains(futureTex.id)) {
                    val existingTex = textures[futureTex.id]!!
                    if(existingTex.width == futureTex.width && existingTex.height == futureTex.height) {
                        if(futureTex.jpeg) {
                            existingTex.setJpeg(futureTex.data)
                        }
                        returnReusableArray(futureTex.data)

                        continue
                    } else {
                        existingTex.delete()
                    }
                }

                if(futureTex.jpeg) {
                    textures[futureTex.id] = textureFactory.createFromJpegBytes(ByteBuffer.wrap(futureTex.data))
                } else {
                    textures[futureTex.id] = textureFactory.create(
                        futureTex.width, futureTex.height, futureTex.data, futureTex.colorSpace
                    )
                }
                returnReusableArray(futureTex.data)
            }
        }

        currentHandler = handler
    }

    private fun returnReusableArray(array: ByteArray) {
        synchronized(reusableArrays) {
            reusableArrays[array.size]?.offer(WeakReference(array))
        }
    }

    fun offerJpeg(id: Int, width: Int, height: Int, data: ByteArray) =
        offer(id, width, height, data, jpeg = true)

    fun offer(id: Int, width: Int, height: Int, data: ByteArray, colorSpace: ColorSpace = ColorSpace.RGB, jpeg: Boolean = false) {
        val size = data.size
        val array: ByteArray

        synchronized(reusableArrays) {
            if(!reusableArrays.contains(size)) {
                array = ByteArray(size)
                reusableArrays[size] = ArrayBlockingQueue(REUSABLE_ARRAY_QUEUE_SIZE)
            } else {
                val queue = reusableArrays[size]!!

                array = if(queue.isEmpty()) {
                    ByteArray(size)
                } else {
                    queue.poll().get() ?: ByteArray(size)
                }
            }

            reusableArrays.remove(size)
        }

        System.arraycopy(data, 0, array, 0, size)

        synchronized(queuedTextures) {
            if(queuedTextures.remainingCapacity() == 0) {
                queuedTextures.poll()
            }

            queuedTextures.offer(FutureTexture(id, width, height, array, colorSpace, jpeg))
        }
    }

    operator fun get(id: Int) = textures[id]

    fun clear() {
        queuedTextures.clear()
        for((_, texture) in textures) {
            texture.delete()
        }
        textures.clear()
    }

    data class FutureTexture(val id: Int, val width: Int, val height: Int, val data: ByteArray, val colorSpace: ColorSpace, val jpeg: Boolean)

}