package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler

sealed class Resolvable<T> {
    data class Now<T>(val result: T) : Resolvable<T>() {
        override fun toString() = result.toString()
        override fun letOrDefer(block: (T) -> Unit) = block(result)
        override fun <R> tryReturn(success: (T) -> R, fail: (String) -> R) = success(result)
        override fun resolve(): T = result
    }

    open class Placeholder<T>(private val resolver: () -> T?) : Resolvable<T>() {
        companion object {
            private val globalPlaceholders = mutableMapOf<String, Placeholder<*>>()
            val registeredPlaceholders get() = globalPlaceholders.keys

            fun fetchPlaceholder(placeholder: String) = globalPlaceholders[placeholder]
        }

        val timestamp = System.currentTimeMillis()
        val placeholder = String.format(CodeGen.RESOLVER_TEMPLATE, timestamp)

        val onResolve = PaperVisionEventHandler("Placeholder-$timestamp-OnResolve", catchExceptions = false)

        init {
            globalPlaceholders[placeholder] = this
        }

        override fun resolve(): T? {
            onResolve()
            return resolver()
        }

        override fun toString() = placeholder

        override fun letOrDefer(block: (T) -> Unit) {
            val value = resolve()
            if (value != null) {
                block(value)
            } else {
                onResolve.doOnce {
                    block(resolve() ?: throw IllegalStateException("Placeholder $placeholder could not be resolved"))
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
    }

    data class DependentPlaceholder<P, T>(val dependency: Resolvable<P>, val resolver: (P) -> T?) : Placeholder<T>({
        val depValue = dependency.resolve()
        if (depValue != null) {
            resolver(depValue)
        } else {
            null
        }
    })

    val value = ConValue(Type.NONE, toString())

    abstract fun letOrDefer(block: (T) -> Unit)
    abstract fun <R> tryReturn(success: (T) -> R, fail: (String) -> R): R

    abstract fun resolve(): T?
}