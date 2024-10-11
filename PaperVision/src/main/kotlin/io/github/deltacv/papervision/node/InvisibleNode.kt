package io.github.deltacv.papervision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

@SerializeIgnore
class InvisibleNode : Node<NoSession>(allowDelete = false) {

    private val invisibleColor = rgbaColor(0, 0, 0, 0)

    override fun draw() {
        ImNodes.pushColorStyle(ImNodesCol.NodeOutline, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBar, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackground, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundSelected, invisibleColor)

        ImNodes.beginNode(id)
        ImNodes.endNode()

        repeat(7) {
            ImNodes.popColorStyle()
        }
    }

    override fun genCode(current: CodeGen.Current) = NoSession

}