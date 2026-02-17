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

@PaperNode(
    name = "nod_decomposer",
    category = NodeCategory.TRANSFORM
)
class DecomposerNode : DrawNode<NoSession>() {

    private var decomposer: AttributeDecomposer<*>? = null

    val input = AnyAttribute(INPUT, "$[att_input]", linkAcceptor = {
        if(it is TypedAttribute<*>) {
            val decomposer = it.attributeType.decomposer(this)

            if(decomposer != null) {
                this.decomposer = decomposer
                Attribute.LinkAcceptance.Accept
            } else Attribute.LinkAcceptance.Reject("err_couldntlink_notdecomposable")
        } else Attribute.LinkAcceptance.Reject
    })

    override fun onEnable() {
        + input

        input.onLink {
            decomposer?.enable()
        }

        input.onUnlink {
            decomposer?.disable()
            decomposer = null
        }
    }

    override val generators = generatorsBuilder {
        generatorForAny { _, current ->
            current.codeGen.sessions[decomposer!!] = decomposer!!.genCode(input.genValue(current), current)
            NoSession
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return decomposer?.getGenValueOf(current, attrib) ?: noValue(attrib)
    }

}