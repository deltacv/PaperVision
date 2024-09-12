package io.github.deltacv.papervision.gui.eocvsim

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.id.IdElementContainer
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.PipelineStream
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
    var stream: PipelineStream
) : DrawNode<NoSession>() {

    val displayId by displayWindows.nextId(this)

    val inputId by IdElementContainerStack.threadStack.peekNonNull<Attribute>().nextId()

    override fun drawNode() {
        ImNodes.pushColorStyle(ImNodesColorStyle.Pin, MatAttribute.styleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.PinHovered, MatAttribute.styleHoveredColor)

        ImNodes.beginInputAttribute(inputId)
        ImNodes.endInputAttribute()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        ImGui.sameLine()

        stream.textureOf(displayId)?.draw()
    }

    override fun delete() {
        super.delete()
        displayWindows.removeId(displayId)
    }

    override fun restore() {
        super.restore()
        displayWindows[displayId] = this
    }

    companion object {
        val displayWindows = IdElementContainer<ImageDisplayNode>()
    }

    override fun genCode(current: CodeGen.Current) = NoSession

}