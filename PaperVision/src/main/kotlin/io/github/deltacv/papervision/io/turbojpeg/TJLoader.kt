package io.github.deltacv.papervision.io.turbojpeg

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

object TJLoader {
    var isLoaded = false

    fun load() {
        // get os and arch
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        val arch = System.getProperty("os.arch").lowercase(Locale.getDefault())

        var libPath: String? = null

        if (os.contains("win")) {
            libPath = if (arch.contains("64")) {
                "/META-INF/lib/windows_64/turbojpeg.dll"
            } else {
                "/META-INF/lib/windows_32/turbojpeg.dll"
            }
        } else if (os.contains("linux")) {
            libPath = if (arch.contains("64")) {
                "/META-INF/lib/linux_64/libturbojpeg.so"
            } else {
                "/META-INF/lib/linux_32/libturbojpeg.so"
            }
        } else if (os.contains("mac") || os.contains("darwin")) {
            libPath = if (arch.contains("64")) {
                "/META-INF/lib/osx_64/libturbojpeg.dylib"
            } else if (arch.contains("ppc")) {
                "/META-INF/lib/osx_ppc/libturbojpeg.dylib"
            } else {
                "/META-INF/lib/osx_32/libturbojpeg.dylib"
            }
        }

        if (libPath == null) {
            isLoaded = false
            throw RuntimeException("Unsupported OS/Arch: $os $arch")
        }

        loadFromResource(libPath)
        isLoaded = true
    }

    private fun loadFromResource(resource: String) {
        try {
            TJLoader::class.java.getResourceAsStream(resource).use { res ->
                if (res == null) {
                    throw RuntimeException("Native lib not found: $resource")
                }
                // Crear archivo temporal
                val tempFile = File.createTempFile("libturbojpeg", getFileExtension(resource))
                tempFile.deleteOnExit() // Eliminar después de ejecución

                // Copiar contenido
                Files.copy(res, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

                // Cargar la biblioteca
                System.load(tempFile.absolutePath)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }


    private fun getFileExtension(path: String): String {
        if (path.endsWith(".dll")) return ".dll"
        if (path.endsWith(".so")) return ".so"
        if (path.endsWith(".dylib")) return ".dylib"
        return ""
    }
}