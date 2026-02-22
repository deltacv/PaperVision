package io.github.deltacv.papervision.node.transform

import io.github.deltacv.papervision.attribute.AnyAttribute
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v1.data.SerializeIgnore

@PaperNode(
    name = "nod_decomposer",
    category = NodeCategory.TRANSFORM,
    description = "des_decomposer"
)
class DecomposerNode : DrawNode<NoSession>() {

    var decomposer: AttributeDecomposer<*>? = null

    @SerializeIgnore
    private var previousLinkedAttribute: Attribute? = null

    val input = AnyAttribute(INPUT, "$[att_attribute]", linkAcceptor = {
        if (it is TypedAttribute<*>) {
            val decomposer = it.attributeType.newDecomposer()

            if (decomposer != null) {
                Attribute.LinkAcceptance.Accept
            } else Attribute.LinkAcceptance.Reject("err_couldntlink_notdecomposable")
        } else Attribute.LinkAcceptance.Reject
    })

    override fun onEnable() {
        + input

        // enable if serialization set it up
        input.availableLinkedAttribute?.let {
            decomposer?.enable(this, it)
        }
    }

    override fun drawNode() {
        val currentLinkedAttribute = input.availableLinkedAttribute

        if(currentLinkedAttribute != previousLinkedAttribute || decomposer == null) {
            decomposer?.disable()
            decomposer = null

            if(currentLinkedAttribute != null) {
                val decomposer = (currentLinkedAttribute as? TypedAttribute<*>)?.attributeType?.newDecomposer()

                if(decomposer != null) {
                    decomposer.enable(this, currentLinkedAttribute)
                    this.decomposer = decomposer
                }
            }
        }

        previousLinkedAttribute = currentLinkedAttribute
    }

    override val generators = generatorsBuilder {
        generatorForAny { _, current ->
            decomposer?.let {
                current.codeGen.sessions[it] = it.genCode(input.genValue(current), current)
            }
            NoSession
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        warnAssert(decomposer != null, "Decomposer is null")
        return decomposer?.getGenValueOf(current, attrib) ?: noValue(attrib)
    }

}