package io.github.deltacv.papervision.io

import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.event.EventHandler
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

class TextureProcessorQueue(
    val textureFactory: PlatformTextureFactory
) {

    private val reusableArrays = mutableMapOf<Int, ArrayBlockingQueue<WeakReference<ByteArray>>>()

    private val queuedTextures = ArrayBlockingQueue<FutureTexture>(5)
    private val textures = mutableMapOf<Int, PlatformTexture>()

    fun subscribeTo(handler: EventHandler) {
        handler {
            while(queuedTextures.isNotEmpty()) {
                val futureTex = queuedTextures.poll()

                if(textures.contains(futureTex.id)) {
                    val existingTex = textures[futureTex.id]!!
                    if(existingTex.width == futureTex.width && existingTex.height == futureTex.height) {
                        existingTex.set(futureTex.data)
                        returnReusableArray(futureTex.data)

                        continue
                    } else {
                        existingTex.delete()
                    }
                }

                textures[futureTex.id] = textureFactory.create(
                    futureTex.width, futureTex.height, futureTex.data
                )
                returnReusableArray(futureTex.data)
            }
        }
    }

    private fun returnReusableArray(array: ByteArray) {
        synchronized(reusableArrays) {
            reusableArrays[array.size]?.offer(WeakReference(array))
        }
    }

    fun offer(id: Int, width: Int, height: Int, data: ByteBuffer) {
        val size = data.remaining()
        val array: ByteArray

        synchronized(reusableArrays) {
            if(!reusableArrays.contains(size)) {
                array = ByteArray(size)
                reusableArrays[size] = ArrayBlockingQueue(5)
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

        System.arraycopy(data.array(), data.position(), array, 0, size)

        synchronized(queuedTextures) {
            if(queuedTextures.remainingCapacity() == 0) {
                Thread.yield()
            }

            queuedTextures.offer(FutureTexture(id, width, height, array))
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

    data class FutureTexture(val id: Int, val width: Int, val height: Int, val data: ByteArray)

}