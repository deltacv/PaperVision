/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui.util

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.id.StatedIdElementBase

class FontManager {

    val fonts = mutableMapOf<String, Font>()

    fun makeFont(
        name: String,
        ttfPath: String,
        fontConfig: ImFontConfig,
        glyphRanges: ShortArray? = null
    ): Font {
        val hashName = "mai-$ttfPath-${fontConfig.sizePixels}"

        if(fonts.containsKey(hashName)) {
            return fonts[hashName]!!
        }

        val inputStream = FontManager::class.java.getResourceAsStream(ttfPath)
            ?: throw IllegalArgumentException("Font file $ttfPath not found in resources")

        inputStream.use {
            val imguiFont = if (glyphRanges != null) {
                ImGui.getIO().fonts.addFontFromMemoryTTF(
                    it.readAllBytes(),
                    fontConfig.sizePixels,
                    fontConfig,
                    glyphRanges
                )
            } else {
                ImGui.getIO().fonts.addFontFromMemoryTTF(it.readAllBytes(), fontConfig.sizePixels, fontConfig)
            }

            val font = Font(
                imguiFont,
                fontConfig,
                name,
                ttfPath,
                fontConfig.sizePixels
            )
            fonts[hashName] = font

            font.enable()

            return font
        }
    }

    fun makeDefaultFont(size: Int): Font {
        val key = "def-$size"
        if(fonts.containsKey(key)) {
            return fonts[key]!!
        }

        val fontConfig = ImFontConfig()
        fontConfig.sizePixels = size.toFloat()
        fontConfig.oversampleH = 1
        fontConfig.oversampleV = 1
        fontConfig.pixelSnapH = false

        val font = Font(ImGui.getIO().fonts.addFontDefault(fontConfig), fontConfig, "default-$size", null, size.toFloat())
        fonts[key] = font

        font.enable()

        return font
    }

}

class Font internal constructor(
    val imfont: ImFont,
    val fontConfig: ImFontConfig,
    val name: String,
    val ttfPath: String?,
    val size: Float
) : StatedIdElementBase<Font>() {
    override val idElementContainer get() = IdElementContainerStack.local.peekNonNull<Font>()
    override val requestedId = name.hashCode()

    companion object {
        fun find(name: String): Font {
            if(!IdElementContainerStack.local.peekNonNull<Font>().has(name)) {
                throw IllegalArgumentException("Font $name not found")
            }

            return IdElementContainerStack.local.peekNonNull<Font>()[name]
        }
    }
}

fun defaultFontConfig(size: Float) = ImFontConfig().apply {
    oversampleH = 2
    oversampleV = 2
    pixelSnapH = false
    sizePixels = size
}