package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.EmptyInputAttribute
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

@PaperNode(
    name = "nod_previewdisplay",
    category = Category.HIGH_LEVEL_CV,
    showInList = false
)
@SerializeIgnore
class ImageDisplayNode(
    val imageDisplay: ImageDisplay
) : DrawNode<NoSession>(joinActionStack = false) {

    val input = EmptyInputAttribute(this)

    override fun drawNode() {
        ImNodes.pushColorStyle(ImNodesCol.Pin, MatAttribute.styleColor)
        ImNodes.pushColorStyle(ImNodesCol.PinHovered, MatAttribute.styleHoveredColor)

        ImNodes.beginInputAttribute(input.id)
        ImNodes.endInputAttribute()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        ImGui.sameLine()

        imageDisplay.drawStream()
    }

    override fun delete() {
        super.delete()
        input.delete()
    }

    override fun genCode(current: CodeGen.Current) = NoSession

}