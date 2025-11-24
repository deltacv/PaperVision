package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.node.DrawNode

class HoughCirclesNode : DrawNode<HoughCirclesNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    override fun onEnable() {
        + input.rebuildOnChange()
    }

    class Session : CodeGenSession {

    }
}