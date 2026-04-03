package org.deltacv.papervision.attribute.decomp

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.AttributeMode
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.GenValueMapper
import org.deltacv.papervision.codegen.PolyglotGenerator
import org.deltacv.papervision.node.Node
import org.deltacv.papervision.serialization.v2.DataCodec
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class AttributeDecomposer<S: CodeGenSession> :
    PolyglotGenerator<GenValue, S>,
    GenValueMapper,
    DataCodec
{

    companion object {
        val OUTPUT = AttributeMode.OUTPUT
    }

    lateinit var decomposerNode: Node<*>
        private set

    lateinit var inputAttribute: Attribute
        private set

    val linkedAttribute get() = inputAttribute.availableLinkedAttribute

    private val outputAttributes = mutableListOf<Attribute>()

    abstract fun onEnable()

    fun enable(decomposerNode: Node<*>, inputAttribute: Attribute) {
        if(::decomposerNode.isInitialized && this.decomposerNode != decomposerNode) {
            throw IllegalStateException("Decomposer cannot be reused")
        }

        this.decomposerNode = decomposerNode
        this.inputAttribute = inputAttribute
        onEnable()
    }

    fun disable() {
        outputAttributes.forEach {
            decomposerNode.removeAttribute(it)
            it.delete()
        }
        outputAttributes.clear()
    }

    operator fun Attribute.unaryPlus() = addOutputAttribute(this)

    fun addOutputAttribute(attribute: Attribute) {
        if(attribute.mode != AttributeMode.OUTPUT) {
            throw IllegalArgumentException("Only output attributes can be added to the decomposer")
        }

        outputAttributes.add(attribute)
        decomposerNode.addAttribute(attribute)

        attribute.enable()
    }

    protected fun noValue(attrib: Attribute): Nothing = decomposerNode.noValue(attrib)

    @OptIn(ExperimentalContracts::class)
    protected inline fun <reified T: GenValue> assertGenValueType(value: GenValue) {
        contract {
            returns() implies (value is T)
        }

        if(value !is T) {
            decomposerNode.raise("Decomposer received invalid GenValue type (expected ${T::class.simpleName} received ${value::class.simpleName})")
        }
    }

}



