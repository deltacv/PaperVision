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

import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.dsl.CodeGenContext
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.resolve.PlaceholderResolver
import io.github.deltacv.papervision.codegen.resolve.Resolvable
import io.github.deltacv.papervision.id.container.IdContainerStacks
import io.github.deltacv.papervision.util.loggerFor

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED, PACKAGE_PRIVATE
}

class CodeGen(
    var className: String,
    val language: Language,
    val isForPreviz: Boolean = false
) {

    companion object {
        val logger by loggerFor<CodeGen>()
    }

    val importScope = Scope(0, language)
    val importScopePlaceholder = Resolvable.Placeholder(resolveLast = true) {
        importScope
    }

    val classStartScope = Scope(1, language, importScope, isForPreviz)
    val classEndScope   = Scope(1, language, importScope, isForPreviz)

    val initScope = Scope(2, language, importScope, isForPreviz)
    val currInit  = Current(this, initScope, isForPreviz)

    val processFrameScope = Scope(2, language, importScope, isForPreviz)
    val currProcessFrame  = Current(this, processFrameScope, isForPreviz)

    val viewportTappedScope = Scope(2, language, importScope, isForPreviz)
    val currViewportTapped  = Current(this, viewportTappedScope, isForPreviz)

    val sessions = mutableMapOf<GenNode<*>, CodeGenSession>()
    val busyNodes = mutableListOf<GenNode<*>>()

    val endingNodes = mutableListOf<GenNode<*>>()

    private val flags = mutableListOf<String>()

    private val placeholderResolver = PlaceholderResolver(importScope)

    enum class Stage {
        CREATION, INITIAL_GEN, END_GEN, ENDED_SUCCESS, ENDED_ERROR
    }

    var stage = Stage.CREATION

    fun gen(): String {
        val raw = language.gen(this)

        return placeholderResolver.resolve(raw, IdContainerStacks.local.peekNonNull<Resolvable.Placeholder<*>>())
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

        fun <S: CodeGenSession> nonNullSessionOf(node: GenNode<S>) = sessionOf(node) ?: run {
            node.genCodeIfNecessary(this)
            this@Current.sessionOf(node)
                ?: throw IllegalStateException("Node ${node::class.simpleName} did not generate a session when requested")
        }

        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

data class CodeGenOptions(var genAtTheEnd: Boolean = false)

interface CodeGenSession
object NoSession : CodeGenSession