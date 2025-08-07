package io.github.deltacv.papervision.io.turbojpeg

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

object TJLoader {
    fun load() {
        // get os and arch
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        val arch = System.getProperty("os.arch").lowercase(Locale.getDefault())

        var libPath: String? = null

        if (os.contains("win")) {
            if (arch.contains("64")) {
                libPath = "/META-INF/lib/windows_64/turbojpeg.dll"
            } else {
                libPath = "/META-INF/lib/windows_32/turbojpeg.dll"
            }
        } else if (os.contains("linux")) {
            if (arch.contains("64")) {
                libPath = "/META-INF/lib/linux_64/libturbojpeg.so"
            } else {
                libPath = "/META-INF/lib/linux_32/libturbojpeg.so"
            }
        } else if (os.contains("mac") || os.contains("darwin")) {
            if (arch.contains("64")) {
                libPath = "/META-INF/lib/osx_64/libturbojpeg.dylib"
            } else if (arch.contains("ppc")) {
                libPath = "/META-INF/lib/osx_ppc/libturbojpeg.dylib"
            } else {
                libPath = "/META-INF/lib/osx_32/libturbojpeg.dylib"
            }
        }

        if (libPath == null) {
            throw RuntimeException("Unsupported OS/Arch: " + os + " " + arch)
        }

        loadFromResource(libPath)
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
                System.load(tempFile.getAbsolutePath())
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