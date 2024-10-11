package io.github.deltacv.papervision.gui.style

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol

interface ImNodesStyle : ImNodesStyleTemplate {
    override fun apply() {
        ImNodes.pushColorStyle(ImNodesCol.NodeBackground, nodeBackground)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundHovered, nodeBackgroundHovered)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundSelected, nodeBackgroundSelected)
        ImNodes.pushColorStyle(ImNodesCol.NodeOutline, nodeOutline)

        ImNodes.pushColorStyle(ImNodesCol.TitleBar, titleBar)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, titleBarHovered)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, titleBarSelected)

        ImNodes.pushColorStyle(ImNodesCol.Link, link)
        ImNodes.pushColorStyle(ImNodesCol.LinkHovered, linkHovered)
        ImNodes.pushColorStyle(ImNodesCol.LinkSelected, linkSelected)

        ImNodes.pushColorStyle(ImNodesCol.Pin, pin)
        ImNodes.pushColorStyle(ImNodesCol.PinHovered, pinHovered)

        ImNodes.pushColorStyle(ImNodesCol.BoxSelector, boxSelector)
        ImNodes.pushColorStyle(ImNodesCol.BoxSelectorOutline, boxSelectorOutline)
    }
}