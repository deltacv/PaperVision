package io.github.deltacv.easyvision.node

import com.beust.klaxon.Json
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.exception.NodeGenException
import io.github.deltacv.easyvision.serialization.NodeSerializationData
import io.github.deltacv.mai18n.tr

interface Type {
    val name: String
}

abstract class Node<S: CodeGenSession, D: NodeSerializationData>(
    private var allowDelete: Boolean = true
) : DrawableIdElement {

    @Json(ignored = true)
    var serializedId: Int? = null
        private set

    @Json(ignored = true)
    var nodesIdContainer = nodes
    @Json(ignored = true)
    var attributesIdContainer = attributes

    @Json(ignored = true)
    var drawAttributesCircles = true

    @Json(ignored = true)
    override val id by lazy {
        if(serializedId == null) {
            nodesIdContainer.nextId(this).value
        } else {
            nodesIdContainer.requestId(this, serializedId!!).value
        }
    }

    @Json(ignored = true)
    private val attribs = mutableListOf<Attribute>() // internal mutable list
    @Json(ignored = true)
    val nodeAttributes = attribs as List<Attribute> // public read-only

    @Json(ignored = true)
    var genSession: S? = null
        private set

    protected fun drawAttributes() {
        for((i, attribute) in nodeAttributes.withIndex()) {
            attribute.draw()

            if(i < nodeAttributes.size - 1 && !attribute.wasLastDrawCancelled) {
                ImGui.newLine() // make a new blank line if this isn't the last attribute
            }
        }
    }

    override fun delete() {
        if(allowDelete) {
            for (attribute in nodeAttributes.toTypedArray()) {
                for(link in attribute.links.toTypedArray()) {
                    link.delete()
                }

                attribute.delete()
                attribs.remove(attribute)
            }

            nodes.removeId(id)
        }
    }

    override fun restore() {
        if(allowDelete) {
            for (attribute in nodeAttributes.toTypedArray()) {
                for(link in attribute.links.toTypedArray()) {
                    link.restore()
                }

                attribute.restore()
                attribs.add(attribute)
            }

            nodes[id] = this
        }
    }

    fun addAttribute(attribute: Attribute) {
        attribute.parentNode = this
        attribs.add(attribute)
    }

    operator fun Attribute.unaryPlus() = addAttribute(this)

    abstract fun genCode(current: CodeGen.Current): S

    open fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        raise("Node doesn't have output attributes")
    }

    @Json(ignored = true)
    private var isCurrentlyGenCode = false

    @Suppress("UNCHECKED_CAST")
    /**
     * Generates code if there's not a session in the current CodeGen
     * Automatically propagates to all the nodes attached to the output
     * attributes after genCode finishes. Called by default on onPropagateReceive()
    */
    fun genCodeIfNecessary(current: CodeGen.Current) {
        val codeGen = current.codeGen
        val session = codeGen.sessions[this]

        if(session == null) {
            // prevents duplicate code in weird edge cases
            // (it's so hard to consider and test every possibility with nodes...)
            if(!isCurrentlyGenCode) {
                isCurrentlyGenCode = true

                genSession = genCode(current)
                codeGen.sessions[this] = genSession!!

                isCurrentlyGenCode = false

                propagate(current)
            }
        } else {
            genSession = session as S
        }
    }

    fun propagate(current: CodeGen.Current) {
        for(attribute in attribs) {
            if(attribute.mode == AttributeMode.OUTPUT) {
                for(linkedAttribute in attribute.linkedAttributes()) {
                    linkedAttribute.parentNode.onPropagateReceive(current)
                }
            }
        }
    }

    protected abstract fun makeSerializationData(): D
    protected open fun takeDeserializationData(data: D) { /* do nothing */ }

    /**
     * Call before enable()
     */
    fun deserialize(data: D) {
        serializedId = data.id
        takeDeserializationData(data)
    }

    fun serialize(): D {
        val data = makeSerializationData()

        data.id = id
        data.node = this

        val pos = ImVec2()
        ImNodes.getNodeEditorSpacePos(id, pos)

        data.nodePos = pos

        return data
    }

    open fun onPropagateReceive(current: CodeGen.Current) {
        genCodeIfNecessary(current)
    }

    fun raise(message: String): Nothing = throw NodeGenException(this, message)

    fun warn(message: String) {
        println("WARN: $message") // TODO: Warnings system...
    }

    fun raiseAssert(condition: Boolean, message: String) {
        if(!condition) {
            raise(message)
        }
    }

    fun warnAssert(condition: Boolean, message: String) {
        if(!condition) {
            warn(message)
        }
    }

    companion object {
        val nodes = IdElementContainer<Node<*, *>>()
        val attributes = IdElementContainer<Attribute>()

        @JvmStatic protected val INPUT = AttributeMode.INPUT
        @JvmStatic protected val OUTPUT = AttributeMode.OUTPUT

        fun checkRecursion(from: Node<*, *>, to: Node<*, *>): Boolean {
            val linksBetween = Link.getLinksBetween(from, to)

            var hasOutputToInput = false
            var hasInputToOutput = false

            for(link in linksBetween) {
                val aNode = link.aAttrib.parentNode

                val fromAttrib = if(aNode == from) link.aAttrib else link.bAttrib
                val toAttrib   = if(aNode == to) link.aAttrib else link.bAttrib

                if(!hasOutputToInput)
                    hasOutputToInput = fromAttrib.mode == OUTPUT && toAttrib.mode == INPUT

                if(!hasInputToOutput)
                    hasInputToOutput = fromAttrib.mode == INPUT && toAttrib.mode == OUTPUT

                if(hasOutputToInput && hasInputToOutput)
                    break
            }

            return hasOutputToInput && hasInputToOutput
        }
    }

}