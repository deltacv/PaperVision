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

package io.github.deltacv.papervision.gui.style

import imgui.ImColor

fun rgbaColor(r: Int, g: Int, b: Int, a: Int) = ImColor.rgba(
    r.toFloat() / 255f,
    g.toFloat() / 255f,
    b.toFloat() / 255f,
    a.toFloat() / 255f
)

fun hexColor(hex: String) = ImColor.rgb(hex)