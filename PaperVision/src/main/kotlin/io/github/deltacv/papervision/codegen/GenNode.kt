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