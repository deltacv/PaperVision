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

import io.github.deltacv.papervision.attribute.Attribute

interface GenNode<S: CodeGenSession> : Generator<S> {

    val genOptions: CodeGenOptions
    var lastGenSession: S?

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
        val codeGen = current.codeGen

        if(genOptions.genAtTheEnd && codeGen.stage != CodeGen.Stage.PRE_END) {
            if(!codeGen.endingNodes.contains(this)) {
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

                lastGenSession = genCode(current)
                codeGen.sessions[this] = lastGenSession!!

                codeGen.busyNodes.remove(this)

                propagate(current)
            }
        } else {
            lastGenSession = session as S
        }
    }

    fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue

}