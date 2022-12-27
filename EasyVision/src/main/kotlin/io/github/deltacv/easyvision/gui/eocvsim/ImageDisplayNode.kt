package io.github.deltacv.easyvision.gui.eocvsim

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.id.IdElementContainerStack
import io.github.deltacv.easyvision.io.PipelineStream
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.serialization.data.SerializeIgnore

@RegisterNode(
    name = "nod_previewdisplay",
    category = Category.HIGH_LEVEL_CV,
    showInList = false
)
@SerializeIgnore
class ImageDisplayNode(
    val stream: PipelineStream
) : DrawNode<NoSession>() {

    val displayId by displayWindows.nextId(this)

    val inputId by IdElementContainerStack.peekNonNull<Attribute>().nextId()

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