package io.github.deltacv.easyvision.gui.style

import imgui.ImColor

fun rgbaColor(r: Int, g: Int, b: Int, a: Int) = ImColor.floatToColor(
    r.toFloat() / 255f,
    g.toFloat() / 255f,
    b.toFloat() / 255f,
    a.toFloat() / 255f
)

fun hexColor(hex: String) = ImColor.rgbToColor(hex)