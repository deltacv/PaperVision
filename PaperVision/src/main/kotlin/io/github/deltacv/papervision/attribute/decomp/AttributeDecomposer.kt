package io.github.deltacv.papervision.attribute.decomp

import com.google.gson.JsonObject
import imgui.ImGui
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.GenValueMapper
import io.github.deltacv.papervision.codegen.PolyglotGenerator
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.serialization.AttributeSerializationData
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.data.SerializeData
import io.github.deltacv.papervision.serialization.data.adapter.dataSerializableToJsonObject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class AttributeDecomposer<S: CodeGenSession> : PolyglotGenerator<GenValue, S>, GenValueMapper, DataSerializable<Any> {

    companion object {
        val INPUT = AttributeMode.INPUT
        val OUTPUT = AttributeMode.OUTPUT
    }

    lateinit var decomposerNode: Node<*>
        private set

    private val _outputAttributes = mutableListOf<Attribute>()

    val outputAttributes get() = _outputAttributes.toList()

    abstract fun onEnable()

    fun enable(decomposerNode: Node<*>) {
        if(::decomposerNode.isInitialized && this.decomposerNode != decomposerNode) {
            throw IllegalStateException("Decomposer cannot be reused")
        }

        this.decomposerNode = decomposerNode
        onEnable()
    }

    fun disable() {
        _outputAttributes.forEach {
            decomposerNode.removeAttribute(it)
            it.delete()
        }
        _outputAttributes.clear()
    }

    operator fun Attribute.unaryPlus() = addOutputAttribute(this)

    fun addOutputAttribute(attribute: Attribute) {
        if(attribute.mode != AttributeMode.OUTPUT)
            throw IllegalArgumentException("Only output attributes can be added to the decomposer")

        _outputAttributes.add(attribute)
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

    final override fun deserialize(data: Any) { }

    final override fun serialize() = Any()

}