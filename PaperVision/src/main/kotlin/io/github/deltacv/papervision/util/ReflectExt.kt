package io.github.deltacv.papervision.util

import java.lang.reflect.Field

fun Class<*>.hasSuperclass(superclass: Class<*>): Boolean {
    return superclass.isAssignableFrom(this)
}

/**
 * Get a field from a class, including superclasses.
 */
fun Class<*>.getFieldDeep(name: String): Field? {
    var field: Field? = null
    var clazz: Class<*>? = this

    while (clazz != null && field == null) {
        try {
            field = clazz.getDeclaredField(name)
        } catch (_: Exception) { }
        clazz = clazz.superclass
    }

    return field
}