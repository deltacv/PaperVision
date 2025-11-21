package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode


@PaperNode(
    name = "PaperVision Magic",
    category = Category.FEATURE_DET,
    description = "epic",
    showInList = false
)
class PaperVisionMagicNode : DrawNode<NoSession>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val output = MatAttribute(OUTPUT, "Magic Output")

    override fun onEnable() {
        + input
        + output
    }

}