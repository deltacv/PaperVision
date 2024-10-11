package io.github.deltacv.papervision.gui.style

import imgui.ImColor

fun rgbaColor(r: Int, g: Int, b: Int, a: Int) = ImColor.rgba(
    r.toFloat() / 255f,
    g.toFloat() / 255f,
    b.toFloat() / 255f,
    a.toFloat() / 255f
)

fun hexColor(hex: String) = ImColor.rgb(hex)