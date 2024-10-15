package io.github.deltacv.papervision.gui

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import io.github.deltacv.papervision.io.copyToTempFile
import java.io.File

class FontManager {

    val fonts = mutableMapOf<String, Font>()

    val ttfFiles = mutableMapOf<String, File>()

    fun makeFont(
        ttfPath: String,
        fontConfig: ImFontConfig,
        glyphRanges: ShortArray? = null
    ): Font {
        val hashName = "mai-$ttfPath-${fontConfig.sizePixels}"

        if(fonts.containsKey(hashName)) {
            return fonts[hashName]!!
        }

        if(!ttfFiles.containsKey(ttfPath) || !ttfFiles[ttfPath]!!.exists()) {
            ttfFiles[ttfPath] = copyToTempFile(
                FontManager::class.java.getResourceAsStream(ttfPath)!!,
                File(ttfPath).name, true
            )
        }

        val file = ttfFiles[ttfPath]!!

        val imguiFont = if(glyphRanges != null) {
            ImGui.getIO().fonts.addFontFromFileTTF(file.absolutePath, fontConfig.sizePixels, fontConfig, glyphRanges)
        } else {
            ImGui.getIO().fonts.addFontFromFileTTF(file.absolutePath, fontConfig.sizePixels, fontConfig)
        }

        val font = Font(
            imguiFont,
            fontConfig,
            ttfPath,
            fontConfig.sizePixels
        )
        fonts[hashName] = font

        return font
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

        val font = Font(ImGui.getIO().fonts.addFontDefault(fontConfig), fontConfig, null, size)
        fonts[name] = font

        return font
    }

}

class Font internal constructor(
    val imfont: ImFont,
    val fontConfig: ImFontConfig,
    val ttfPath: String?,
    val size: Float
)

fun defaultFontConfig(size: Float) = ImFontConfig().apply {
    oversampleH = 2
    oversampleV = 2
    pixelSnapH = false
    sizePixels = size
}