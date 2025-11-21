package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.Resolvable.Now
import io.github.deltacv.papervision.codegen.Resolvable.Placeholder

fun <T> T.resolved() = Resolvable.Now(this)

fun <T> Resolvable.Companion.from(resolver: () -> T?): Resolvable<T> {
    val value = resolver()
    return if (value != null) {
        Now(value)
    } else {
        Placeholder(resolver = resolver)
    }
}

fun <T> Resolvable.Companion.fromResolvable(resolver: () -> Resolvable<T>?) = from {
    val resolved = resolver()
    val value = resolved?.resolve()
    value
}