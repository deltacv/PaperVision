package io.github.deltacv.easyvision.attribute

import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.exception.AttributeGenException
import io.github.deltacv.easyvision.id.DrawableIdElement
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.serialization.ev.AttributeSerializationData
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.ev.BasicAttribData

enum class AttributeMode { INPUT, OUTPUT }

abstract class Attribute : DrawableIdElement, DataSerializable<AttributeSerializationData> {

    private var serializedId: Int? = null

    abstract val mode: AttributeMode

    override val id by lazy {
        if(serializedId == null) {
            parentNode.attributesIdContainer.nextId(this).value
        } else {
            parentNode.attributesIdContainer.requestId(this, serializedId!!).value
        }
    }

    lateinit var parentNode: Node<*>
        internal set

    val links = mutableListOf<Link>()
    val hasLink get() = links.isNotEmpty()

    val isInput by lazy { mode == AttributeMode.INPUT }
    val isOutput by lazy { !isInput }

    private var isFirstDraw = true
    private var cancelNextDraw = false

    var wasLastDrawCancelled = false
        private set

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

        if(wasLastDrawCancelled) {
            wasLastDrawCancelled = false
        }

        if(isFirstDraw) {
            enable()
            isFirstDraw = false
        }

        if(parentNode.drawAttributesCircles) {
            if (mode == AttributeMode.INPUT) {
                ImNodes.beginInputAttribute(id)
            } else {
                ImNodes.beginOutputAttribute(id)
            }
        }

        drawAttribute()

        if(parentNode.drawAttributesCircles) {
            if (mode == AttributeMode.INPUT) {
                ImNodes.endInputAttribute()
            } else {
                ImNodes.endOutputAttribute()
            }
        }
    }

    override fun delete() {
        Node.attributes.removeId(id)

        for(link in links.toTypedArray()) {
            link.delete()
            links.remove(link)
        }
    }

    override fun restore() {
        Node.attributes[id] = this
    }

    fun linkedAttribute(): Attribute? {
        if(!isInput) {
            raise("Output attributes might have more than one link, so linkedAttribute() is not allowed")
        }

        if(!hasLink) {
            return null
        }

        val link = links[0]

        return if(link.aAttrib == this) {
            link.bAttrib
        } else link.aAttrib
    }

    fun linkedAttributes() = links.map {
        if(it.aAttrib == this) {
            it.bAttrib
        } else it.aAttrib
    }

    fun raise(message: String): Nothing = throw AttributeGenException(this, message)

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

    abstract fun acceptLink(other: Attribute): Boolean

    abstract fun value(current: CodeGen.Current): GenValue

    protected fun getOutputValue(current: CodeGen.Current) = parentNode.getOutputValueOf(current, this)

    open fun makeSerializationData() = BasicAttribData(id)
    open fun takeDeserializationData(data: AttributeSerializationData) { /* do nothing */ }

    /**
     * Call before enable()
     */
    final override fun deserialize(data: AttributeSerializationData) {
        serializedId = data.id

        takeDeserializationData(data)
    }

    final override fun serialize(): AttributeSerializationData {
        val data = makeSerializationData()
        data.id = id

        return data
    }

    override fun toString() = "Attribute(type=${this::class.java.typeName}, id=$id)"

}