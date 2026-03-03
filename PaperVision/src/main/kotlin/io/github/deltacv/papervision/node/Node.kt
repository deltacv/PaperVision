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

package io.github.deltacv.papervision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.rebuildOnLink
import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.editor.NodeEditor
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.container.IdContainerStacks
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.v1.data.DataSerializable
import io.github.deltacv.papervision.serialization.v1.BasicNodeData
import io.github.deltacv.papervision.serialization.v1.NodeSerializationData
import io.github.deltacv.papervision.serialization.v2.CodecTypeRegistry
import io.github.deltacv.papervision.serialization.v2.DataCodec
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder
import io.github.deltacv.papervision.util.DelegatedChangeEmitter
import io.github.deltacv.papervision.util.QueuedChangeEmitter
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.event.PaperEventListenerId
import io.github.deltacv.papervision.util.loggerFor
import io.github.deltacv.papervision.util.loggerForThis
import org.deltacv.mai18n.tr
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface Type {
    val name: String
}

abstract class Node<S: CodeGenSession>(
    allowDelete: Boolean = true,
    val joinActionStack: Boolean = true,
    val rebuildOnLink: Boolean = true
) : DrawableIdElementBase<Node<*>>(),
    GenNode<S>,
    GenValueMapper,
    DelegatedChangeEmitter<Node.ChangeType>,
    DataSerializable<NodeSerializationData>,
    DataCodec
{

    private val logger by loggerForThis()

    override val idContainer = IdContainerStacks.local.peekNonNull<Node<*>>()
    override val requestedId get() = if(forgetSerializedId) null else serializedId

    private var beforeDeletingPosition = ImVec2()

    val description by lazy { this::class.java.getAnnotation(PaperNode::class.java)?.description }

    var allowDelete = allowDelete
        private set

    var serializedId: Int? = null
        private set

    var showAttributesCircles = true

    var forgetSerializedId = false
        private set

    // will be set on NodeEditor#draw
    lateinit var editor: NodeEditor
        internal set

    val isOnEditor get() = ::editor.isInitialized && idContainer.contains(this)

    // it is the responsibility of the inheriting class to set this value in draw()
    val screenPosition = ImVec2()
    val gridPosition = ImVec2()
    val position = ImVec2()
    val size = ImVec2()

    override val generators = mutableMapOf<PolyglotMapping, Generator<Unit, S>>()

    override val genOptions = CodeGenOptions()

    val onDelete = PaperEventHandler("OnDelete-${this::class.simpleName}")

    private val attribOnChangeListenerIds = mutableMapOf<Attribute, PaperEventListenerId>()

    @Transient
    private val _nodeAttributes = mutableListOf<Attribute>() // internal mutable list

    val nodeAttributes = _nodeAttributes as List<Attribute> // public read-only

    override val changeEmitterDelegate = QueuedChangeEmitter<ChangeType>(defaultPeekChange = ChangeType.AttributeChange)

    protected open fun drawAttributes(additional: List<Attribute> = emptyList()) {
        val total = (nodeAttributes + additional)

        for((i, attribute) in total.withIndex()) {
            attribute.parentNode = this
            attribute.draw()

            if(i < total.size - 1 && !attribute.wasLastDrawCancelled) {
                ImGui.newLine() // make a new blank line if this isn't the last attribute
            }
        }
    }

    override fun delete() {
        if(allowDelete) {
            forceDelete()
        }
    }

    fun forceDelete() {
        beforeDeletingPosition = ImVec2(position)

        for (attribute in nodeAttributes.toTypedArray()) {
            attribute.delete()
        }

        idContainer.removeId(id)
        onDelete.run()
    }

    override fun restore() {
        if(allowDelete) {
            forceRestore()
        }
    }

    fun forceRestore() {
        for (attribute in nodeAttributes.toTypedArray()) {
            attribute.restore()
        }

        idContainer[id] = this

        if(this is DrawNode<*>) {
            nextNodePosition = beforeDeletingPosition
        }
    }

    open fun addAttribute(attribute: Attribute) {
        if(!_nodeAttributes.contains(attribute)) {
            attribute.parentNode = this
            attribOnChangeListenerIds[attribute] = attribute.onChange {
                // emit this attribute's changes to the node
                emitChange(ChangeType.AttributeChange)
            }

            if(rebuildOnLink) {
                // automatically rebuild previz when this attribute's links change
                attribute.rebuildOnLink()
            }

            attribute.enable()

            _nodeAttributes.add(attribute)
        }
    }

    fun removeAttribute(attribute: Attribute) {
        if(_nodeAttributes.contains(attribute)) {
            //attribute.parentNode = null
            attribOnChangeListenerIds.remove(attribute)?.let {
                attribute.onChange.removeListener(it)
            }
            _nodeAttributes.remove(attribute)
        }
    }

    operator fun Attribute.unaryPlus() = addAttribute(this)

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        raise("Node does not have output attributes ('getGenValueOf' was not overridden)")
    }

    fun hasDeadEnd(initialNode: Node<*> = this): Boolean {
        for(attribute in nodeAttributes) {
            if(attribute.mode == AttributeMode.INPUT) {
                continue
            }

            for(linkedAttribute in attribute.availableLinkedAttributes) {
               if(linkedAttribute != null) {
                   if(linkedAttribute.mode == AttributeMode.OUTPUT || linkedAttribute.parentNode == initialNode) {
                       continue // uh oh
                   }

                   if(linkedAttribute.parentNode is OutputMatNode || !linkedAttribute.parentNode.hasDeadEnd(initialNode)) {
                       return false // not a dead end
                   }
               }
            }
        }

        return true
    }

    override fun codeGenPropagate(current: CodeGen.Current) {
        val linkedNodes = mutableListOf<Node<*>>()

        for(attribute in _nodeAttributes) {
            if(attribute.mode == AttributeMode.OUTPUT) {
                for(linkedAttribute in attribute.availableLinkedAttributes) {
                    if(linkedAttribute != null && !linkedNodes.contains(linkedAttribute.parentNode)) {
                        linkedNodes.add(linkedAttribute.parentNode)
                    }
                }
            }
        }

        val deadEndNodes = mutableListOf<Node<*>>()

        for(linkedNode in linkedNodes) {
            if(linkedNode.hasDeadEnd()) {
                deadEndNodes.add(linkedNode)
                logger.debug("Dead end: {}", linkedNode)
            } else {
                logger.debug("Part of the chain: {}", linkedNode)
            }
        }

        // Propagate to dead ends, so they can be processed without being left out
        // because no one depends on them, as they won't be propagated to otherwise.
        deadEndNodes.forEach { it.genCodeIfNecessary(current) }
    }

    // ------------------ Serialization v1 ------------------

    open fun takeSerializationData(data: NodeSerializationData) { /* do nothing */ }
    open fun makeSerializationData() = BasicNodeData(id, ImNodes.getNodeEditorSpacePos(id))

    /**
     * Call before enable()
     */
    final override fun deserialize(data: NodeSerializationData) {
        serializedId = data.id
        if(this is DrawNode<*>) {
            nextNodePosition = data.nodePos
        }

        takeSerializationData(data)
    }

    final override fun serialize(): NodeSerializationData {
        val data = makeSerializationData()
        data.id = id

        data.nodePos = ImNodes.getNodeEditorSpacePos(id)

        return data
    }

    // ------------------ Serialization v2 ------------------

    override fun encode(encoder: DataEncoder) {
        encoder.int("id", id)
    }

    override fun decode(decoder: DataDecoder) {
        serializedId = decoder.int("id")
        logger.debug("Decoded node with id {} ({})", serializedId, CodecTypeRegistry.nameOf(this::class))
    }

    fun noValue(attrib: Attribute): Nothing {
        val name = (attrib as? TypedAttribute<*>)?.variableName ?: attrib::class.simpleName ?: attrib.toString()
        raise(tr("err_attrib_nothandled_bythis", tr(name)))
    }

    fun raise(message: String): Nothing = throw NodeGenException(this, message)

    fun warn(message: String) {
        logger.warn("CODE GEN WARN: $message") // TODO: Warnings system...
    }

    @OptIn(ExperimentalContracts::class)
    fun raiseAssert(condition: Boolean, message: String) {
        contract {
            returns() implies condition
        }

        if(!condition) {
            raise(message)
        }
    }

    fun warnAssert(condition: Boolean, message: String) {
        if(!condition) {
            warn(message)
        }
    }

    fun forgetSerializedId() {
        forgetSerializedId = true
    }

    override fun toString() = "Node(\"$genNodeName\", id=$id)"

    sealed class ChangeType {
        object AttributeChange: ChangeType()
    }

    companion object {
        private val logger by loggerFor<Node<*>>()

        @JvmStatic protected val INPUT = AttributeMode.INPUT
        @JvmStatic protected val OUTPUT = AttributeMode.OUTPUT

        fun instantiateNode(nodeClazz: Class<out Node<*>>): Node<*>? = try {
            nodeClazz.getConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            logger.warn("Node class ${nodeClazz.name} does not have a no-arg constructor", e)
            null
        } catch (e: Exception) {
            logger.warn("Error instantiating node class ${nodeClazz.name}", e)
            null
        }

        fun checkSimpleRecursion(from: Node<*>, to: Node<*>): Boolean {
            val linksBetween = Link.getLinksBetween(from, to)

            var hasOutputToInput = false
            var hasInputToOutput = false

            for(link in linksBetween) {
                val aNode = link.aAttrib?.parentNode ?: continue

                val fromAttrib = (if(aNode == from) link.aAttrib else link.bAttrib) ?: continue
                val toAttrib   = (if(aNode == to) link.aAttrib else link.bAttrib) ?: continue

                if(!hasOutputToInput)
                    hasOutputToInput = fromAttrib.mode == OUTPUT && toAttrib.mode == INPUT

                if(!hasInputToOutput)
                    hasInputToOutput = fromAttrib.mode == INPUT && toAttrib.mode == OUTPUT

                if(hasOutputToInput && hasInputToOutput)
                    return true
            }

            return false
        }
    }
}
