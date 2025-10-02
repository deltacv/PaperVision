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

package io.github.deltacv.papervision.attribute

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.exception.AttributeGenException
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.serialization.AttributeSerializationData
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.BasicAttribData
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import java.util.concurrent.ArrayBlockingQueue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

enum class AttributeMode { INPUT, OUTPUT }

class EmptyInputAttribute(
    parent: Node<*>? = null
) : Attribute() {
    override val mode = AttributeMode.INPUT

    init {
        parent?.let {
            parentNode = it
        }
    }

    override fun drawAttribute() {
    }

    override fun acceptLink(other: Attribute) = true

    override fun genValue(current: CodeGen.Current): GenValue {
        throw NotImplementedError("value() is not implemented for EmptyInputAttribute")
    }
}

abstract class Attribute : DrawableIdElementBase<Attribute>(), DataSerializable<AttributeSerializationData> {

    override val idElementContainer get() = IdElementContainerStack.localStack.peekNonNull<Attribute>()

    override val requestedId get() = if(forgetSerializedId || (hasParentNode && parentNode.forgetSerializedId))
        null
    else serializedId

    private var serializedId: Int? = null

    abstract val mode: AttributeMode

    lateinit var parentNode: Node<*>
        internal set

    val hasParentNode get() = ::parentNode.isInitialized

    val links = mutableListOf<Link>()
    val enabledLinks get() = links.filter { it.isEnabled }

    val hasLink get() = enabledLinks.isNotEmpty()

    val isInput by lazy { mode == AttributeMode.INPUT }
    val isOutput by lazy { !isInput }

    val isOnEditor get() = parentNode.isOnEditor
    val editor get() = parentNode.editor

    var showAttributesCircles = true

    var forgetSerializedId = false
        private set

    private var isFirstDraw = true
    private var cancelNextDraw = false

    var wasLastDrawCancelled = false
        private set

    val onChange = PaperVisionEventHandler("OnChange-${this::class.simpleName}").apply {
        doPersistent {
            changeQueue.add(true)
        }
    }
    val onDelete = PaperVisionEventHandler("OnDelete-${this::class.simpleName}")

    val position = ImVec2()
    val editorPosition = ImVec2()

    internal val changeQueue = ArrayBlockingQueue<Boolean>(5)

    abstract fun drawAttribute()

    fun drawHere() {
        draw()
        cancelNextDraw = true
    }

    override fun draw() {
        if(cancelNextDraw) {
            cancelNextDraw = false
            wasLastDrawCancelled = true
            return
        }

        if(changeQueue.remainingCapacity() <= 1) {
            changeQueue.poll()
        }

        if(wasLastDrawCancelled) {
            wasLastDrawCancelled = false
        }

        if(isFirstDraw) {
            enable()
            isFirstDraw = false
            onChange.run()
        }

        if(parentNode.showAttributesCircles && showAttributesCircles) {
            if (mode == AttributeMode.INPUT) {
                ImNodes.beginInputAttribute(id)
            } else {
                ImNodes.beginOutputAttribute(id)
            }
            ImGui.getCursorPos(position)
        }

        drawAttribute()

        if(parentNode.showAttributesCircles && showAttributesCircles) {
            if (mode == AttributeMode.INPUT) {
                ImNodes.endInputAttribute()
            } else {
                ImNodes.endOutputAttribute()
            }
        }
    }

    override fun delete() {
        onDelete.run()
        idElementContainer.removeId(id)

        for(link in enabledLinks.toTypedArray()) {
            link.delete()
        }
    }

    override fun restore() {
        idElementContainer[id] = this

        for(link in links.toTypedArray()) {
            if(link.getOtherAttribute(this)?.isEnabled == true)
                link.restore()
        }
    }

    val availableLinkedAttribute: Attribute? get() {
        if(!isInput) {
            raise("Output attributes might have more than one link, use linkedAttributes instead")
        }

        if(!hasLink) {
            return null
        }

        val link = enabledLinks[0]

        return if(link.aAttrib == this) {
            link.bAttrib
        } else link.aAttrib
    }

    val allLinkedAttributes get() = enabledLinks.map {
        if(it.aAttrib == this) {
            it.bAttrib
        } else it.aAttrib
    }

    val availableLinkedAttributes get() = enabledLinks.map {
        if(it.aAttrib == this) {
            it.bAttrib
        } else it.aAttrib
    }

    fun raise(message: String): Nothing = throw AttributeGenException(this, message)

    fun warn(message: String) {
        println("WARN: $message") // TODO: Warnings system...
    }

    fun requireAttachedAttribute() = raiseAssert(hasLink, "err_musthave_attachedattrib")

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

    abstract fun acceptLink(other: Attribute): Boolean

    abstract fun genValue(current: CodeGen.Current): GenValue

    internal open fun readEditorValue(): Any? = null

    val editorValue get() = when {
        mode == AttributeMode.INPUT -> readEditorValue()
        hasLink -> availableLinkedAttribute!!.readEditorValue()
        else -> null
    }

    fun rebuildPreviz() {
        if(!isOnEditor) return

        // schedule for a frame later
        editor.paperVision.onUpdate.doOnce {
            parentNode.editor.paperVision.previzManager.refreshPreviz()
        }
    }

    fun getGenValueFromNode(current: CodeGen.Current) = parentNode.getGenValueOf(current, this)

    open fun makeSerializationData(): AttributeSerializationData = BasicAttribData(id)
    open fun takeSerializationData(data: AttributeSerializationData) { /* do nothing */ }

    /**
     * Call before enable()
     */
    final override fun deserialize(data: AttributeSerializationData) {
        serializedId = data.id
        takeSerializationData(data)
    }

    final override fun serialize(): AttributeSerializationData {
        val data = makeSerializationData()
        data.id = id

        return data
    }

    fun forgetSerializedId() {
        forgetSerializedId = true
    }

    override fun pollChange() = changeQueue.poll() ?: false

    override fun toString() = "Attribute(type=${this::class.java.typeName}, id=$id)"

}

fun <T: Attribute> T.rebuildOnChange(): T = apply {
    onChange {
        if(idElementContainer[id] != null) {
            rebuildPreviz()
        }
    }
}