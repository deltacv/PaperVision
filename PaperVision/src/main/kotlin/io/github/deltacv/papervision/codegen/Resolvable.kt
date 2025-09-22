package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.id.IdElement
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler

sealed class Resolvable<T> {

    companion object {
        fun <T> from(resolver: () -> T?): Resolvable<T> {
            val value = resolver()
            return if (value != null) {
                Now(value)
            } else {
                Placeholder(resolver = resolver)
            }
        }

        fun <T> fromResolvable(resolver: () -> Resolvable<T>?) = from {
            val resolved = resolver()
            val value = resolved?.resolve()
            value
        }
    }

    /* -- abstract Resolvable members -- */

    val value get() = ConValue(Type.NONE, toString())

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
        val placeholder get() = String.format(CodeGen.RESOLVER_TEMPLATE, id)

        private var usingOnResolve = false // to avoid creating the event handler if not necessary
        val onResolve by lazy {
            usingOnResolve = true
            PaperVisionEventHandler("Placeholder-$placeholder-OnResolve", catchExceptions = false) 
        }

        private var cachedValue: T? = null

        constructor(resolver: () -> T?) : this(resolveLast = false, resolver = resolver)

        override fun resolve(): T? {
            val value = cachedValue ?: resolver()
            if (value != null && usingOnResolve) {
                onResolve()
            }

            cachedValue = value
            return value
        }

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

        override fun toString() = placeholder

        override val id by IdElementContainerStack.localStack.peekNonNull<Placeholder<*>>().nextId(this)
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