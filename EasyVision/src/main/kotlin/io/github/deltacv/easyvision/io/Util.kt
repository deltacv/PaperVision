package io.github.deltacv.easyvision.io

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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