package io.github.deltacv.easyvision.gui.style.imnodes

import imgui.ImColor
import io.github.deltacv.easyvision.gui.style.ImNodesStyle
import io.github.deltacv.easyvision.gui.style.rgbaColor

object ImNodesDarkStyle : ImNodesStyle {
    override val nodeBackground = rgbaColor(50, 50, 50, 255)
    override val nodeBackgroundHovered = rgbaColor(75, 75, 75, 255)
    override val nodeBackgroundSelected = rgbaColor(75, 75, 75, 255)
    override val nodeOutline = rgbaColor(100, 100, 100, 255)

    override val titleBar = rgbaColor(41, 74, 122, 255)
    override val titleBarHovered = rgbaColor(66, 150, 250, 255)
    override val titleBarSelected = rgbaColor(66, 150, 250, 255)

    override val link = rgbaColor(61, 133, 224, 200)
    override val linkHovered = rgbaColor(66, 150, 250, 255)
    override val linkSelected = rgbaColor(66, 150, 250, 255)

    override val pin = rgbaColor(53, 150, 250, 180)
    override val pinHovered = rgbaColor(53, 150, 250, 255)

    override val boxSelector = rgbaColor(61, 133, 224, 30)
    override val boxSelectorOutline = rgbaColor(61, 133, 224, 150)
}