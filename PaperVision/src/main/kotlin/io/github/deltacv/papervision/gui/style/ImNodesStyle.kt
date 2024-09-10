package io.github.deltacv.papervision.gui.style

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle

interface ImNodesStyle : ImNodesStyleTemplate {
    override fun apply() {
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackground, nodeBackground)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackgroundHovered, nodeBackgroundHovered)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackgroundSelected, nodeBackgroundSelected)
        ImNodes.pushColorStyle(ImNodesColorStyle.NodeOutline, nodeOutline)

        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBar, titleBar)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarHovered, titleBarHovered)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarSelected, titleBarSelected)

        ImNodes.pushColorStyle(ImNodesColorStyle.Link, link)
        ImNodes.pushColorStyle(ImNodesColorStyle.LinkHovered, linkHovered)
        ImNodes.pushColorStyle(ImNodesColorStyle.LinkSelected, linkSelected)

        ImNodes.pushColorStyle(ImNodesColorStyle.Pin, pin)
        ImNodes.pushColorStyle(ImNodesColorStyle.PinHovered, pinHovered)

        ImNodes.pushColorStyle(ImNodesColorStyle.BoxSelector, boxSelector)
        ImNodes.pushColorStyle(ImNodesColorStyle.BoxSelectorOutline, boxSelectorOutline)
    }
}