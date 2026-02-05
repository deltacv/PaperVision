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

import io.github.deltacv.papervision.id.IdElement
import io.github.deltacv.papervision.id.container.IdContainerStacks
import io.github.deltacv.papervision.util.event.PaperEventHandler
import kotlin.getValue

sealed class Resolvable<T> {

    companion object {
        const val RESOLVER_PREFIX = "<mack!"
        const val RESOLVER_SUFFIX = ">"

        // %s is the ID of the placeholder
        const val RESOLVER_TEMPLATE = "$RESOLVER_PREFIX%d$RESOLVER_SUFFIX"
    }

    /* -- abstract Resolvable members -- */

    abstract fun letOrDefer(block: (T) -> Unit)
    abstract fun <R> tryReturn(success: (T) -> R, fail: (String) -> R): R
    abstract fun resolve(): T?
    
    fun <R> convertTo(converter: (T?) -> R?): Resolvable<R> = from { converter(resolve()) }

    /* -- IMPLEMENTATIONS -- */

    data class Now<T>(val result: T) : Resolvable<T>() {
        override fun letOrDefer(block: (T) -> Unit) = block(result)
        override fun <R> tryReturn(success: (T) -> R, fail: (String) -> R) = success(result)
        override fun resolve(): T = result

        override fun toString() = result.toString()
    }

    open class Placeholder<T>(
        val resolveLast: Boolean,
        private val resolver: () -> T?
    ) : Resolvable<T>(), IdElement {

        val placeholder get() = String.format(RESOLVER_TEMPLATE, id)

        private var usingOnResolve = false // to avoid creating the event handler if not necessary
        val onResolve by lazy {
            usingOnResolve = true
            PaperEventHandler("Placeholder-$placeholder-OnResolve", catchExceptions = false)
        }

        private var resolving = false

        private var cachedValue: T? = null

        constructor(resolver: () -> T?) : this(resolveLast = false, resolver = resolver)

        override fun resolve(): T? {
            if (resolving) return cachedValue
            resolving = true

            val value = cachedValue ?: resolver()
            if (value != null && usingOnResolve) {
                onResolve.run()
            }

            cachedValue = value
            resolving = false
            return value
        }

        override fun letOrDefer(block: (T) -> Unit) {
            val value = resolve()
            if (value != null) {
                block(value)
            } else {
                onResolve.once {
                    val resolved = cachedValue ?: resolver()
                    if (resolved != null) {
                        cachedValue = resolved
                        block(resolved)
                    } else {
                        throw IllegalStateException("Placeholder $placeholder could not be resolved")
                    }
                }
            }
        }

        override fun <R> tryReturn(success: (T) -> R, fail: (String) -> R): R {
            val result = resolve()

            return if(result != null) {
                success(result)
            } else {
                fail(toString())
            }
        }

        override fun toString() = placeholder

        override val id by IdContainerStacks.local.peekNonNull<Placeholder<*>>().nextIdLazy(this)
    }

    data class DependentPlaceholder<P, T>(val dependency: Resolvable<P>, val resolver: (P) -> T?) : Placeholder<T>(resolver = {
        val depValue = dependency.resolve()
        if (depValue != null) {
            resolver(depValue)
        } else {
            null
        }
    })

    data class DoubleDependentPlaceholder<P1, P2, T>(
        val dependency1: Resolvable<P1>,
        val dependency2: Resolvable<P2>,
        val resolver: (P1, P2) -> T?
    ) : Placeholder<T>(resolver = {
        val depValue1 = dependency1.resolve()
        val depValue2 = dependency2.resolve()
        if (depValue1 != null && depValue2 != null) {
            resolver(depValue1, depValue2)
        } else {
            null
        }
    })
}
