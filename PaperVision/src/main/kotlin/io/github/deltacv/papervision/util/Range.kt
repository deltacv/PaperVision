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

package io.github.deltacv.papervision.util

data class Range2i(val min: Int, val max: Int) {
    fun clip(x: Int) = if(x < min) min else if(x > max) max else x
}
data class Range2d(val min: Double, val max: Double) {
    fun clip(x: Double) = if(x < min) min else if(x > max) max else x
}

fun clip(x: Int, min: Int, max: Int): Int = if(x < min) min else if(x > max) max else x