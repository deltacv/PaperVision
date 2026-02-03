package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.Resolvable.Now
import io.github.deltacv.papervision.codegen.Resolvable.Placeholder
import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Type

val Resolvable<*>.v get() = resolve().let { result ->
    result as? ConValue ?: ConValue(Type.NONE, result.toString())
}

fun <T> T.resolved() = Now(this)

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
