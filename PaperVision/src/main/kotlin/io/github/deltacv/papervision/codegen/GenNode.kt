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

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.util.loggerForThis

interface GenNode<S: CodeGenSession> : Generator<S> {

    val genOptions: CodeGenOptions
    var lastGenSession: S?

    val genNodeName: String?
        get() = null

    fun propagate(current: CodeGen.Current)

    fun receivePropagation(current: CodeGen.Current) {
        genCodeIfNecessary(current)
    }

    /**
     * Generates code if there's not a session in the current CodeGen
     * Automatically propagates to all the nodes attached to the output
     * attributes after genCode finishes. Called by default on onPropagateReceive()
     */
    @Suppress("UNCHECKED_CAST")
    fun genCodeIfNecessary(current: CodeGen.Current) {
        val logger = loggerForThis().value

        val codeGen = current.codeGen

        if(genOptions.genAtTheEnd && codeGen.stage != CodeGen.Stage.END_GEN) {
            if(!codeGen.endingNodes.contains(this)) {
                logger.debug("Marked $this as an ending node")
                codeGen.endingNodes.add(this)
            }

            return
        }

        val session = codeGen.sessions[this]

        if(session == null) {
            // prevents duplicate code in weird edge cases
            // (it's so hard to consider and test every possibility with nodes...)
            if(!codeGen.busyNodes.contains(this)) {
                codeGen.busyNodes.add(this)

                val name = genNodeName

                logger.info("Generating code for ${name ?: this}")

                lastGenSession = genCode(current)

                codeGen.sessions[this] = lastGenSession!!

                codeGen.busyNodes.remove(this)

                logger.info("DONE generating code for ${name ?: this}")

                propagate(current)
            }
        } else {
            lastGenSession = session as S
        }
    }

    fun Scope.nameComment() {
        val name = genNodeName
        if(name != null) {
            comment("\"$name\"")
        }
    }

    fun ScopeContext.nameComment() = scope.nameComment()

    fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue

}
