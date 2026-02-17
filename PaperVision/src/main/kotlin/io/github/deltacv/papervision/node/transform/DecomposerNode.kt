package io.github.deltacv.papervision.node.transform

import io.github.deltacv.papervision.attribute.AnyAttribute
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_decomposer",
    category = NodeCategory.TRANSFORM
)
class DecomposerNode : DrawNode<NoSession>() {

    var decomposer: AttributeDecomposer<*, *>? = null
        set(value) {
            field?.disable()

            value?.enable()
            field = value
        }

    val input = AnyAttribute(INPUT, "$[att_input]", linkAcceptor = {
        if(it.mode == OUTPUT && it is TypedAttribute<*>) {
            val decomposer = it.attributeType.decomposer(this)
            if(decomposer != null) {
                this.decomposer = decomposer
                true
            } else false
        } else false
    })

    override fun onEnable() {
        + input
    }

}