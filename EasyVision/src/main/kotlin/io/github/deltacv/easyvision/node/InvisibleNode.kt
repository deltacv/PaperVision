package io.github.deltacv.easyvision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.gui.style.rgbaColor
import io.github.deltacv.easyvision.serialization.data.SerializeIgnore

@SerializeIgnore
class InvisibleNode : Node<NoSession>(allowDelete = false) {

    private val invisibleColor = rgbaColor(0, 0, 0, 0)

    override fun draw() {
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeOutline, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBar, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarSelected, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackground, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackgroundHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackgroundSelected, invisibleColor)

        ImNodes.beginNode(id)
        ImNodes.endNode()

        repeat(7) {
            ImNodes.popColorStyle()
        }
    }

    override fun genCode(current: CodeGen.Current) = NoSession

}