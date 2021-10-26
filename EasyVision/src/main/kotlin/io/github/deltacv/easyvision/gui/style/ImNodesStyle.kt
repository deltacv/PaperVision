package io.github.deltacv.easyvision.gui.style

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle

interface ImNodesStyle : Style {
    val nodeBackground: Int
    val nodeBackgroundHovered: Int
    val nodeBackgroundSelected: Int
    val nodeOutline: Int

    val titleBar: Int
    val titleBarHovered: Int
    val titleBarSelected: Int

    val link: Int
    val linkHovered: Int
    val linkSelected: Int

    val pin: Int
    val pinHovered: Int

    val boxSelector: Int
    val boxSelectorOutline: Int

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