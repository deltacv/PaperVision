/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui

import imgui.*
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesEditorContext
import imgui.extension.imnodes.flag.ImNodesCol
import imgui.flag.*
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.gui.util.FrameWidthWindow
import io.github.deltacv.papervision.gui.util.Table
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.id.IdElementContainer
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.node.*
import io.github.deltacv.papervision.platform.PlatformWindow
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags
import io.github.deltacv.papervision.util.loggerForThis

typealias CategorizedNodes = Map<Category, MutableList<Class<out Node<*>>>>

class NodeList(
    val paperVision: PaperVision,
    val keyManager: KeyManager,
    val nodeClasses: CategorizedNodes
): Window() {

    companion object {
        const val PLUS_FONT_SIZE = 60f
    }

    val listNodes = IdElementContainer<Node<*>>()
    val listAttributes = IdElementContainer<Attribute>()

    val logger by loggerForThis()

    val Keys = keyManager.keys

    var isNodesListOpen = false
        private set
    private var isCompletelyDeleted = false

    private val openButtonTimeout = ElapsedTime()

    lateinit var floatingButton: FloatingButton
        private set
    lateinit var headers: Headers
        private set
    
    private lateinit var listContext: ImNodesEditorContext

    var hoveredNode = -1
        private set

    var isHoverManuallyDetected = false
        private set
    var isHoveringScrollBar = false
        private set

    private val drawnNodes = mutableListOf<Int>()

    override var title = "list"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration
    )

    override fun onEnable() {
        paperVision.onUpdate {
            if(isCompletelyDeleted) {
                it.removeThis()
            } else if(!paperVision.nodeEditor.isNodeFocused && keyManager.released(Keys.Spacebar)) {
                showList()
            }
        }

        floatingButton = FloatingButton(this, paperVision.window, paperVision.defaultFontBig, paperVision.fontAwesomeBig)
        floatingButton.enable()

        floatingButton.onPressed {
            if (!isNodesListOpen && openButtonTimeout.millis > 200) {
                showList()
            }
        }

        IdElementContainerStack.threadStack.push(listNodes)
        IdElementContainerStack.threadStack.push(listAttributes)

        headers = Headers(keyManager, paperVision.defaultFontBig) { nodes }

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()
    }

    override fun preDrawContents() {
        if(!isNodesListOpen) {
            return
        }

        val size = paperVision.window.size

        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.70f) // transparent dark nodes list window
    }

    override fun drawContents() {
        if(!isNodesListOpen) {
            closeList()
            return
        }

        IdElementContainerStack.threadStack.push(listNodes)
        IdElementContainerStack.threadStack.push(listAttributes)

        val size = paperVision.window.size

        if(keyManager.released(Keys.Escape)) {
            closeList()
        }

        ImNodes.editorContextSet(listContext)

        // NODES WINDOW

        ImNodes.getStyle().gridSpacing = 99999f // lol
        ImNodes.pushColorStyle(ImNodesCol.GridBackground, ImColor.rgba(0f, 0f, 0f, 0f))

        ImNodes.clearNodeSelection()
        ImNodes.clearLinkSelection()

        ImNodes.beginNodeEditor()
        for(category in Category.values()) {
            if(nodes.containsKey(category)) {
                val table = headers.tablesCategories[category] ?: continue

                if (headers.categoriesState[category] == true) {
                    for (node in nodes[category]!!) {
                        if(drawnNodes.contains(node.id)) {
                            if (!table.contains(node.id)) {
                                val nodeSize = ImVec2()
                                ImNodes.getNodeDimensions(nodeSize, node.id)

                                table.add(node.id, nodeSize)
                            } else {
                                val pos = table.getPos(node.id)!!
                                ImNodes.setNodeScreenSpacePos(node.id, pos.x, pos.y)
                            }
                        }

                        var titleColor = 0

                        if(isHoverManuallyDetected && hoveredNode == node.id) {
                            if(node.description != null) {
                                ImGui.pushFont(paperVision.defaultFontBig.imfont)

                                ImGui.beginTooltip()
                                    ImGui.textUnformatted(tr(node.description!!))
                                ImGui.endTooltip()

                                ImGui.popFont()
                            }

                            if(node is DrawNode<*>) {
                                titleColor = node.titleColor
                                node.titleColor = node.titleHoverColor
                            } else {
                                ImNodes.pushColorStyle(ImNodesCol.TitleBar, PaperVision.imnodesStyle.titleBarHovered)
                            }

                            ImNodes.pushColorStyle(ImNodesCol.NodeBackground, PaperVision.imnodesStyle.nodeBackgroundHovered)
                        }

                        node.fontAwesome = paperVision.fontAwesome
                        node.draw()

                        if(!drawnNodes.contains(node.id)) {
                            drawnNodes.add(node.id)
                        }

                        if(isHoverManuallyDetected && hoveredNode == node.id) {

                            if(node is DrawNode<*>) {
                                node.titleColor = titleColor
                            } else {
                                ImNodes.popColorStyle()
                            }

                            ImNodes.popColorStyle()
                        }
                    }
                }
            }
        }

        ImNodes.endNodeEditor()
        ImGui.popStyleColor()

        ImNodes.editorContextResetPanning(0f, 0f)

        val mousePos = ImGui.getMousePos()

        isHoveringScrollBar = mousePos.x >= (size.x - 15f)

        hoveredNode = ImNodes.getHoveredNode()
        isHoverManuallyDetected = false

        if(hoveredNode < 0) {
            tableLoop@ for((_, table) in headers.tablesCategories) {
                for((id, rect) in table.currentRects) {
                    // AABB collision check with any node
                    if(mousePos.x > rect.min.x && mousePos.x < rect.max.x + 6 &&
                        mousePos.y > rect.min.y && mousePos.y < rect.max.y) {
                        hoveredNode = id
                        isHoverManuallyDetected = true

                        break@tableLoop
                    }
                }
            }
        }

        ImNodes.getNodeEditorSpacePos(9999)

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()

        floatingButton.focus = isNodesListOpen && !isHoveringScrollBar

        headers.size = size

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()

        handleClick(!headers.isHeaderHovered)
    }

    private fun handleClick(closeOnClick: Boolean) {
        if(ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            if(hoveredNode >= 0) {
                val instance = paperVision.nodeEditor.addNode(
                    listNodes[hoveredNode]!!::class.java
                ) // add node with the class by using reflection

                if(instance is DrawNode<*>) {
                    val nodePos = ImNodes.getNodeScreenSpacePos(hoveredNode)

                    val mousePos = ImGui.getMousePos()

                    val newPosX = mousePos.x - nodePos.x
                    val newPosY = mousePos.y - nodePos.y

                    instance.nextNodePosition = ImVec2(newPosX, newPosY)
                    instance.pinToMouse = true
                }

                closeList()
            } else if(closeOnClick && !isHoveringScrollBar) { // don't close when the scroll bar is clicked
                closeList()
            }
        }
    }

    override fun delete() {
        super.delete()
        headers.delete()
    }

    override fun restore() {
        super.restore()
        headers.restore()
    }

    fun showList() {
        if(!isNodesListOpen) {
            if(::listContext.isInitialized) {
                ImNodes.editorContextFree(listContext)
            }
            listContext = ImNodes.editorContextCreate()

            isNodesListOpen = true
            restore()
        }
    }

    fun closeList() {
        isNodesListOpen = false
        openButtonTimeout.reset()
        floatingButton.focus = false

        delete()
    }

    fun completelyDelete() {
        isCompletelyDeleted = true

        delete()
        floatingButton.delete()
    }

    val nodes by lazy {
        val map = mutableMapOf<Category, MutableList<Node<*>>>()

        for((category, nodeClasses) in nodeClasses) {
            val list = mutableListOf<Node<*>>()

            for(nodeClass in nodeClasses) {
                if(nodeClass.getAnnotation(PaperNode::class.java)?.showInList == false) {
                    continue
                }

                val instance = try {
                    instantiateNode(nodeClass)
                } catch(e: UnsupportedOperationException) {
                    logger.warn("Skipping node", e)
                    continue
                }

                //instance.drawAttributesCircles = false
                instance.enable()

                list.add(instance)
            }

            if(list.isNotEmpty()) {
                map[category] = list
            }
        }

        map
    }

    class FloatingButton(
        val nodeList: NodeList,
        val window: PlatformWindow,
        val defaultFontBig: Font,
        val fontAwesome: Font,
    ) : FrameWidthWindow() {

        override var title = "floating"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.AlwaysAutoResize
        )

        private var lastButton = false
        private val hoveringPlusTime = ElapsedTime()

        override var frameWidth = 0f

        val onPressed = PaperVisionEventHandler("FloatingButton-OnPressed")

        override fun preDrawContents() {
            position = ImVec2(
                window.size.x - PLUS_FONT_SIZE * 2f, window.size.y - PLUS_FONT_SIZE * 2f
            )
        }

        override fun drawContents() {
            focus = false

            ImGui.pushFont(fontAwesome.imfont)

            frameWidth = ImGui.getFrameHeight() * 1.3f

            val button = ImGui.button(if(nodeList.isNodesListOpen) "x" else FontAwesomeIcons.Plus, frameWidth, frameWidth)

            ImGui.popFont()

            if(ImGui.isItemHovered()) {
                if(hoveringPlusTime.millis > 500) {
                    val tooltipText = if(nodeList.isNodesListOpen) "mis_nodeslist_close" else "mis_nodeslist_open"

                    ImGui.pushFont(defaultFontBig.imfont)

                    ImGui.beginTooltip()
                        ImGui.text(tr(tooltipText))
                    ImGui.endTooltip()

                    ImGui.popFont()
                }
            } else {
                hoveringPlusTime.reset()
            }

            // falling edge detector
            if(lastButton != button && button) {
                onPressed.run()
            }

            lastButton = button
        }

    }

    class Headers(
        val keyManager: KeyManager,
        val headerFont: Font,
        val nodesSupplier: () -> Map<Category, List<Node<*>>>
    ) : Window() {

        override var title = "headers"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.AlwaysVerticalScrollbar
        )

        val tablesCategories = mutableMapOf<Category, Table>()
        val categoriesState = mutableMapOf<Category, Boolean>()

        var currentScroll = 0f
        private var previousScroll = 0f

        var isHeaderHovered = false
            private set

        val Keys = keyManager.keys

        override fun preDrawContents() {
            ImGui.setNextWindowPos(0f, 0f)
            ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.0f) // transparent headers window
        }

        override fun drawContents() {
            val scrollValue = when {
                keyManager.pressing(Keys.ArrowUp) -> {
                    -0.8f
                }
                keyManager.pressing(Keys.ArrowDown) -> {
                    0.8f
                }
                else -> {
                    -ImGui.getIO().mouseWheel
                }
            }

            ImGui.setCursorPos(0f, 0f) // draw the node editor on top of the collapisng headers

            isHeaderHovered = false

            for(category in Category.values()) {
                if(nodesSupplier().containsKey(category)) {
                    if (!tablesCategories.containsKey(category)) {
                        tablesCategories[category] = Table()
                    }

                    ImGui.pushStyleColor(ImGuiCol.Header, category.color)
                    ImGui.pushStyleColor(ImGuiCol.HeaderActive, category.colorSelected)
                    ImGui.pushStyleColor(ImGuiCol.HeaderHovered, category.colorSelected)

                    ImGui.pushFont(headerFont.imfont)

                    val isOpen = ImGui.collapsingHeader(
                        tr(category.properName), ImGuiTreeNodeFlags.DefaultOpen
                    )

                    ImGui.popFont()

                    categoriesState[category] = isOpen

                    ImGui.popStyleColor()
                    ImGui.popStyleColor()
                    ImGui.popStyleColor()

                    if (ImGui.isItemHovered()) {
                        isHeaderHovered = true
                    }

                    if(isOpen) {
                        val table = tablesCategories[category]!!

                        if(previousScroll != currentScroll) {
                            currentScroll = ImGui.getScrollY() + scrollValue * 20.0f
                            ImGui.setScrollY(currentScroll)
                        } else {
                            currentScroll = ImGui.getScrollY()
                        }

                        ImGui.newLine()
                        ImGui.indent(10f)

                        table.draw()

                        ImGui.newLine()
                        ImGui.unindent(10f)
                    }
                }
            }

            ImGui.popStyleColor()

            previousScroll = scrollValue
        }

    }

}