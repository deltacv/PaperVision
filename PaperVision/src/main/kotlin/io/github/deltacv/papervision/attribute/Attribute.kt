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
import io.github.deltacv.papervision.serialization.ev.AttributeSerializationData
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.ev.BasicAttribData
import io.github.deltacv.papervision.util.event.EventHandler
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

enum class AttributeMode { INPUT, OUTPUT }

abstract class Attribute : DrawableIdElementBase<Attribute>(), DataSerializable<AttributeSerializationData> {

    override val idElementContainer get() = IdElementContainerStack.threadStack.peekNonNull<Attribute>()
    override val requestedId get() = serializedId

    @Transient private var getThisSupplier: (() -> Any)? = null

    private var serializedId: Int? = null

    abstract val mode: AttributeMode

    lateinit var parentNode: Node<*>
        internal set

    val links = mutableListOf<Link>()
    val hasLink get() = links.isNotEmpty()

    val isInput by lazy { mode == AttributeMode.INPUT }
    val isOutput by lazy { !isInput }

    val isOnEditor get() = parentNode.isOnEditor
    val editor get() = parentNode.editor

    var showAttributesCircles = true

    private var isFirstDraw = true
    private var cancelNextDraw = false

    var wasLastDrawCancelled = false
        private set

    val onChange = EventHandler("OnChange-${this::class.simpleName}")
    val onDelete = EventHandler("OnDelete-${this::class.simpleName}")

    val position = ImVec2()

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

        for(link in links.toTypedArray()) {
            link.delete()
            links.remove(link)
        }
    }

    override fun restore() {
        idElementContainer[id] = this
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

    fun thisGetTo(supplier: () -> Any) {
        getThisSupplier = supplier
    }

    open fun thisGet(): Any? = throw IllegalStateException("This attribute can't return a get() value")

    fun get(): Any? = when {
        mode == AttributeMode.INPUT -> thisGet()
        hasLink -> linkedAttribute()!!.get()
        else -> (getThisSupplier ?: throw IllegalStateException("This attribute can't return a get() value")).invoke()
    }

    @OptIn(ExperimentalContracts::class)
    fun getIfPossible(orElse: () -> Unit): Any? {
        contract {
            callsInPlace(orElse, InvocationKind.AT_MOST_ONCE)
        }

        return try {
            get()
        } catch (ignored: IllegalStateException) {
            orElse()
            null
        }
    }

    fun rebuildPreviz() { }

    protected fun getOutputValue(current: CodeGen.Current) = parentNode.getOutputValueOf(current, this)

    open fun makeSerializationData(): AttributeSerializationData = BasicAttribData(id)
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

fun <T: Attribute> T.rebuildOnChange(): T = apply {
    onChange {
        if(idElementContainer[id] != null) {
            rebuildPreviz()
        }
    }
}