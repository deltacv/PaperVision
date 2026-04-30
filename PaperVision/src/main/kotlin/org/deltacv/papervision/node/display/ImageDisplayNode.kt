package org.deltacv.papervision.node.display

import org.deltacv.papervision.attribute.EmptyInputAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.NoSession
import org.deltacv.papervision.gui.display.ImageDisplay
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v1.data.SerializeIgnore
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_previewdisplay",
    category = NodeCategory.HIGH_LEVEL_CV,
    showInList = false,
    instantiable = false
)
class ImageDisplayNode(
    val imageDisplay: ImageDisplay
) : DrawNode<NoSession>(joinActionStack = false) {

    val input = object: EmptyInputAttribute(this) {
        override fun drawAttribute() {
            imageDisplay.draw()
        }
    }

    override fun onEnable() {
        + input
    }

    override fun genCode(input: Unit, current: CodeGen.Current) = NoSession

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.ignore()
    }

}