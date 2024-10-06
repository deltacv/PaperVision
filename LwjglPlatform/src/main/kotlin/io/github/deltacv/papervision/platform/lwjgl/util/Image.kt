package io.github.deltacv.papervision.platform.lwjgl.util

import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
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

fun ImageData.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val pixels = (image.raster.dataBuffer as DataBufferInt).data

    buffer.rewind()

    for(i in 0 until width * height) {
        val r = buffer.get().toInt() and 0xFF
        val g = buffer.get().toInt() and 0xFF
        val b = buffer.get().toInt() and 0xFF
        val a = buffer.get().toInt() and 0xFF

        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    return image
}