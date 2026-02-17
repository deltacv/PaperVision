package io.github.deltacv.papervision.attribute.decomp

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.GenValueMapper
import io.github.deltacv.papervision.codegen.PolyglotGenerator
import io.github.deltacv.papervision.node.Node
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class AttributeDecomposer<S: CodeGenSession>(val decomposerNode: Node<*>) : PolyglotGenerator<GenValue, S>, GenValueMapper {

    companion object {
        val INPUT = AttributeMode.INPUT
        val OUTPUT = AttributeMode.OUTPUT
    }

    private val outputAttributes = mutableListOf<Attribute>()

    abstract fun onEnable()

    fun enable() {
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
        if(attribute.mode != AttributeMode.OUTPUT)
            throw IllegalArgumentException("Only output attributes can be added to the decomposer")

        outputAttributes.add(attribute)
        decomposerNode.addAttribute(attribute)
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