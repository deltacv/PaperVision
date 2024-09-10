package io.github.deltacv.papervision.util

val Any.hexString get() = Integer.toHexString(hashCode())!!

fun flags(vararg flags: Int) = if(flags.isNotEmpty()) {
    var composedFlags = flags[0]

    for(i in 1 until flags.size) {
        composedFlags = composedFlags or flags[i]
    }

    composedFlags
} else 0