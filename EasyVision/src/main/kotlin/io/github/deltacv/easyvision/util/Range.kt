package io.github.deltacv.easyvision.util

data class Range2i(val min: Int, val max: Int)
data class Range2d(val min: Double, val max: Double)

fun clip(x: Int, min: Int, max: Int): Int = if(x < min) min else if(x > max) max else x