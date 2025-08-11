/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.build.PlaceholderGenValueResolver
import io.github.deltacv.papervision.codegen.build.PlaceholderResolver
import io.github.deltacv.papervision.codegen.build.GenPlaceholderValue
import io.github.deltacv.papervision.codegen.build.PlaceholderValue
import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.dsl.CodeGenContext
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.util.hexString

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED
}

class CodeGen(
    var className: String,
    val language: Language,
    val isForPreviz: Boolean = false
) {

    companion object {
        // %s is the hexcode of the placeholder
        val RESOLVER_TEMPLATE = "<mack!%s>"
    }

    val importScope     = Scope(0, language)
    val classStartScope = Scope(1, language, importScope, isForPreviz)
    val classEndScope   = Scope(1, language, importScope, isForPreviz)

    val initScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeInit = Current(this, initScope, isForPreviz)

    val processFrameScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeProcessFrame = Current(this, processFrameScope, isForPreviz)

    val viewportTappedScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeViewportTapped = Current(this, viewportTappedScope, isForPreviz)

    val sessions = mutableMapOf<GenNode<*>, CodeGenSession>()
    val busyNodes = mutableListOf<GenNode<*>>()

    val endingNodes = mutableListOf<GenNode<*>>()

    var registeredPlaceholderResolvers = mutableListOf<PlaceholderResolver>()

    private val flags = mutableListOf<String>()

    enum class Stage {
        CREATION, INITIAL_GEN, END_GEN, ENDED_SUCCESS, ENDED_ERROR
    }

    var stage = Stage.CREATION

    fun gen() = resolveAllPlaceholders(language.gen(this))

    private fun resolveAllPlaceholders(preprocessed: String): String {
        var resolved = preprocessed

        registeredPlaceholderResolvers.forEach { resolver ->
            val placeholder = RESOLVER_TEMPLATE.format(resolver.hexString)
            val value = resolver()

            resolved = resolved.replace(placeholder, value.value ?: "")
            importScope.importType(value.type)
        }

        return resolved
    }

    fun addFlag(flag: String) = if(!flags.contains(flag)) flags.add(flag) else false
    fun hasFlag(flag: String) = flags.contains(flag)
    fun flags() = flags.toTypedArray()

    val context = CodeGenContext(this)

    operator fun <T> invoke(block: CodeGenContext.() -> T) = block(context)

    data class Current(val codeGen: CodeGen, val scope: Scope, val isForPreviz: Boolean) {
        val language get() = codeGen.language

        @Suppress("UNCHECKED_CAST")
        fun <S: CodeGenSession> sessionOf(node: GenNode<S>) = codeGen.sessions[node] as S?

        fun <S: CodeGenSession> nonNullSessionOf(node: GenNode<S>) = sessionOf(node) ?: {
            node.genCodeIfNecessary(this)
            sessionOf(node) ?: throw IllegalStateException("Node ${node::class.simpleName} did not generate a session")
        }()

        fun <G: GenValue> makeGenPlaceholder(genValueResolver: () -> G, valueResolver: (G) -> Value): GenPlaceholderValue<G> {
            val resolver = PlaceholderGenValueResolver(genValueResolver, valueResolver)
            codeGen.registeredPlaceholderResolvers.add(resolver)

            return GenPlaceholderValue(resolver)
        }

        fun makePlaceholder(valueResolver: (PlaceholderResolver) -> Value): PlaceholderValue {
            val resolver = PlaceholderResolver(valueResolver)
            codeGen.registeredPlaceholderResolvers.add(resolver)

            return PlaceholderValue(resolver)
        }

        fun <G: GenValue, S: CodeGenSession> getGenValueOrMakePlaceholder(
            node: GenNode<S>,
            sessionGenValueResolver: (S) -> G,
            placeholderValueResolver: (G) -> Value,
            placeholderGenValueResolver: (GenPlaceholderValue<G>) -> G
        ): G {
            val session = sessionOf(node)
            return if(session != null) {
                sessionGenValueResolver(session)
            } else {
                placeholderGenValueResolver(
                    makeGenPlaceholder({
                        val session = nonNullSessionOf(node)
                        sessionGenValueResolver(session)
                    }, placeholderValueResolver)
                )
            }
        }

        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

class CodeGenOptions {
    var genAtTheEnd = false
    inline operator fun invoke(block: CodeGenOptions.() -> Unit) = block()
}

interface CodeGenSession

object NoSession : CodeGenSession