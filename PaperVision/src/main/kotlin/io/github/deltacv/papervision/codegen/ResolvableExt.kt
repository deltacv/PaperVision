/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
