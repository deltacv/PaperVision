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

import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.dsl.CodeGenContext
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.id.IdContainerStacks
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

    val importScope            = Scope(0, language)
    val importScopePlaceholder = Resolvable.Placeholder(resolveLast = true) {
        importScope
    }

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

    private val flags = mutableListOf<String>()

    enum class Stage {
        CREATION, INITIAL_GEN, END_GEN, ENDED_SUCCESS, ENDED_ERROR
    }

    var stage = Stage.CREATION

    fun gen() = resolveAllPlaceholders(language.gen(this))

    private fun resolveAllPlaceholders(preprocessed: String): String {
        var resolved = preprocessed

        val placeholders = IdContainerStacks.local
            .peekNonNull<Resolvable.Placeholder<*>>()

        logger.info("Resolving active placeholders: ${placeholders.inmutable.size}")

        // Initial debug log of all placeholders
        placeholders.inmutable.forEach {
            val v = it.resolve()
            logger.debug("${it.placeholder} = $v")
        }

        fun resolve(currentPlaceholdersProvider: () -> Collection<Resolvable.Placeholder<*>>) {
            do {
                var changed = false
                val sb = StringBuilder()
                var i = 0

                // Resolve all once per pass
                currentPlaceholdersProvider().forEach { it.resolve() }

                while (i < resolved.length) {
                    val start = resolved.indexOf(Resolvable.RESOLVER_PREFIX, i)
                    if (start == -1) {
                        // No more placeholders
                        sb.append(resolved, i, resolved.length)
                        break
                    }

                    val end = resolved.indexOf(Resolvable.RESOLVER_SUFFIX, start)
                    if (end == -1) {
                        // No closing '>', append rest as-is
                        sb.append(resolved, i, resolved.length)
                        break
                    }

                    // Append text before the placeholder
                    sb.append(resolved, i, start)

                    // Extract the number part
                    val idPart = resolved.substring(start + 6, end) // after "<mack!"
                    val id = idPart.toIntOrNull()

                    if (id != null) {
                        val placeholder = currentPlaceholdersProvider().find { it.id == id }
                        if (placeholder != null) {
                            val value = placeholder.resolve()
                            val replacement = if (value is Value) {
                                importScope.importType(value.type)
                                value.value ?: ""
                            } else {
                                value.toString()
                            }
                            sb.append(replacement)
                            changed = true
                        } else {
                            // Keep the placeholder as-is if no match found
                            sb.append(resolved, start, end + 1)
                        }
                    } else {
                        // Not a valid number after <mack!
                        sb.append(resolved, start, end + 1)
                    }

                    i = end + 1
                }

                resolved = sb.toString()
            } while (changed) // Repeat until no changes, meaning all placeholders were resolved
        }

        resolve { placeholders.inmutable.filter { !it.resolveLast } } // Resolve non-last placeholders first
        resolve { placeholders.inmutable.filter { it.resolveLast } } // Then resolve last placeholders

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

        fun <S: CodeGenSession> nonNullSessionOf(node: GenNode<S>) = sessionOf(node) ?: run {
            node.genCodeIfNecessary(this)
            this@Current.sessionOf(node)
                ?: throw IllegalStateException("Node ${node::class.simpleName} did not generate a session")
        }

        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

data class CodeGenOptions(var genAtTheEnd: Boolean = false)

interface CodeGenSession

object NoSession : CodeGenSession
