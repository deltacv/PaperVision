package io.github.deltacv.easyvision.gui

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import io.github.deltacv.easyvision.io.copyToTempFile
import io.github.deltacv.easyvision.io.fileExtension
import java.io.File

class FontManager {

    val fonts = mutableMapOf<String, Font>()

    val ttfFiles = mutableMapOf<String, File>()

    fun makeFont(ttfPath: String, name: String, size: Float): Font {
        if(fonts.containsKey(name)) {
            return fonts[name]!!
        }

        val hashName = "$name-$size"
        if(fonts.containsKey(hashName)) {
            return fonts[hashName]!!
        }

        if(!ttfFiles.containsKey(name)) {
            ttfFiles[name] = copyToTempFile(
                FontManager::class.java.getResourceAsStream(ttfPath),
                "$name.${ttfPath.fileExtension}", true
            )
        }

        val file = ttfFiles[name]!!

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size
        fontConfig.oversampleH = 1
        fontConfig.oversampleV = 1
        fontConfig.pixelSnapH = false

        val font = Font(
            ImGui.getIO().fonts.addFontFromFileTTF(file.absolutePath, size, fontConfig),
            name, size
        )
        fonts[hashName] = font

        return font
    }

    fun resizeFont(font: Font, newSize: Float): Font {
        if(font.isDefault) {
            return makeDefaultFont(newSize)
        } else {
            return makeFont("", font.name, newSize)
        }
    }

    fun makeDefaultFont(size: Float): Font {
        val name = "default-$size"
        if(fonts.containsKey(name)) {
            return fonts[name]!!
        }

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size
        fontConfig.oversampleH = 1
        fontConfig.oversampleV = 1
        fontConfig.pixelSnapH = false

        val font = Font(ImGui.getIO().fonts.addFontDefault(fontConfig), name, size, true)
        fonts[name] = font

        return font
    }

}

class Font internal constructor(val imfont: ImFont, val name: String, val size: Float, val isDefault: Boolean = false)