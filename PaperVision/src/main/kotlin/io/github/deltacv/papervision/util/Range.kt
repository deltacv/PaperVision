package io.github.deltacv.papervision.util

data class Range2i(val min: Int, val max: Int) {
    fun clip(x: Int) = if(x < min) min else if(x > max) max else x
}
data class Range2d(val min: Double, val max: Double) {
    fun clip(x: Double) = if(x < min) min else if(x > max) max else x
}

fun clip(x: Int, min: Int, max: Int): Int = if(x < min) min else if(x > max) max else x