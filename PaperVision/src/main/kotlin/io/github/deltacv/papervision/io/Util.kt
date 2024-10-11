package io.github.deltacv.papervision.io

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import java.awt.image.BufferedImage

import java.awt.Image
import java.awt.image.DataBufferByte
import javax.imageio.ImageIO

fun copyToFile(inp: InputStream, file: File, replaceIfExisting: Boolean = true) {
    if(file.exists() && !replaceIfExisting) return

    Files.copy(inp, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

fun copyToTempFile(inp: InputStream, name: String, replaceIfExisting: Boolean = true): File {
    val tmpDir = System.getProperty("java.io.tmpdir")
    val tempFile = File(tmpDir + File.separator + name)

    copyToFile(inp, tempFile, replaceIfExisting)

    return tempFile
}

val String.fileExtension: String? get() {
    return if(contains(".")) {
        substring(lastIndexOf(".") + 1)
    } else {
        null
    }
}

fun resourceToString(resourcePath: String): String {
    val stream = KeyManager::class.java.getResourceAsStream(resourcePath)!!
    return stream.bufferedReader().use { it.readText() }
}

fun bufferedImageFromResource(resourcePath: String): BufferedImage = ImageIO.read(
    KeyManager::class.java.getResourceAsStream(resourcePath)!!
)

fun BufferedImage.bytes(): ByteArray = (raster.dataBuffer as DataBufferByte).data

fun BufferedImage.scaleToFit(newWidth: Int, newHeight: Int): BufferedImage {
    val scalex = newWidth.toDouble() / width
    val scaley = newHeight.toDouble() / height
    val scale = scalex.coerceAtMost(scaley)

    val w = (width * scale).toInt()
    val h = (height * scale).toInt()

    val tmp = getScaledInstance(w, h, Image.SCALE_SMOOTH)

    val resized = BufferedImage(w, h, type)
    val g2d = resized.createGraphics()

    g2d.drawImage(tmp, 0, 0, null)
    g2d.dispose()

    tmp.flush()

    return resized
}