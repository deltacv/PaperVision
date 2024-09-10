package io.github.deltacv.papervision.gui

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import io.github.deltacv.papervision.io.copyToTempFile
import java.io.File

class FontManager {

    val fonts = mutableMapOf<String, Font>()

    val ttfFiles = mutableMapOf<String, File>()

    fun makeFont(ttfPath: String, size: Float): Font {
        val hashName = "mai-$ttfPath-$size"

        if(fonts.containsKey(hashName)) {
            return fonts[hashName]!!
        }

        if(!ttfFiles.containsKey(ttfPath) || !ttfFiles[ttfPath]!!.exists()) {
            ttfFiles[ttfPath] = copyToTempFile(
                FontManager::class.java.getResourceAsStream(ttfPath),
                File(ttfPath).name, true
            )
        }

        val file = ttfFiles[ttfPath]!!

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size
        fontConfig.oversampleH = 2
        fontConfig.oversampleV = 2
        fontConfig.pixelSnapH = false

        val font = Font(
            ImGui.getIO().fonts.addFontFromFileTTF(file.absolutePath, size, fontConfig),
            ttfPath, size
        )
        fonts[hashName] = font

        return font
    }

    fun resizeFont(font: Font, newSize: Float): Font {
        return if(font.ttfPath == null) {
            makeDefaultFont(newSize)
        } else {
            makeFont(font.ttfPath, newSize)
        }
    }

    fun makeDefaultFont(size: Float): Font {
        val name = "def-$size"
        if(fonts.containsKey(name)) {
            return fonts[name]!!
        }

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size
        fontConfig.oversampleH = 1
        fontConfig.oversampleV = 1
        fontConfig.pixelSnapH = false

        val font = Font(ImGui.getIO().fonts.addFontDefault(fontConfig), null, size)
        fonts[name] = font

        return font
    }

}

class Font internal constructor(
    val imfont: ImFont,
    val ttfPath: String?,
    val size: Float
)