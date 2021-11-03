package io.github.deltacv.easyvision.platform.lwjgl.util

import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

fun loadImageFromResource(resourcePath: String): ImageData {
    val imgBuffer = try {
        IOUtil.ioResourceToByteBuffer(resourcePath, 8 * 1024)
    } catch(e: Exception) {
        throw RuntimeException("Exception while loading image $resourcePath", e)
    }

    var image: ImageData? = null

    MemoryStack.stackPush().use {
        val comp = it.mallocInt(1)
        val w = it.mallocInt(1)
        val h = it.mallocInt(1)

        val img = stbi_load_from_memory(imgBuffer, w, h, comp, 4)
            ?: throw RuntimeException("Failed to load image $resourcePath due to ${stbi_failure_reason()}")

        image = ImageData(img, w.get(), h.get())
    }

    return image!!
}

data class ImageData(val buffer: ByteBuffer, val width: Int, val height: Int)