package io.github.deltacv.easyvision.gui

import imgui.*
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.flag.*
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.gui.util.Table
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.mai18n.tr
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.node.*
import io.github.deltacv.easyvision.platform.PlatformWindow
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.easyvision.util.event.EventHandler
import io.github.deltacv.easyvision.util.flags
import kotlinx.coroutines.*

class NodeList(val easyVision: EasyVision, val keyManager: KeyManager): Window() {

    companion object {
        val listNodes = IdElementContainer<Node<*>>()
        val listAttributes = IdElementContainer<Attribute>()

        const val plusFontSize = 60f
    }

    var isNodesListOpen = false
        private set
    private var isCompletelyDeleted = false

    private val openButtonTimeout = ElapsedTime()

    lateinit var floatingButton: FloatingButton
        private set
    lateinit var headers: Headers
        private set
    
    private lateinit var listContext: ImNodesContext

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
        easyVision.onUpdate {
            if(isCompletelyDeleted) {
                it.removeThis()
            } else if(!easyVision.nodeEditor.isNodeFocused && keyManager.released(Keys.Spacebar)) {
                showList()
            }
        }

        floatingButton = FloatingButton(this, easyVision.window, easyVision.fontManager)
        floatingButton.enable()

        floatingButton.onPressed {
            if (!isNodesListOpen && NodeScanner.hasFinishedAsyncScan && openButtonTimeout.millis > 200) {
                showList()
            }
        }

        headers = Headers(keyManager) { nodes }
    }

    override fun preDrawContents() {
        if(!isNodesListOpen) {
            return
        }

        val size = easyVision.window.size

        ImGui.setNextWindowPos(0f, 0f)
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.55f) // transparent dark nodes list window
    }

    override fun drawContents() {
        if(!isNodesListOpen) {
            closeList()
            return
        }

        val size = easyVision.window.size

        if(keyManager.released(Keys.Escape)) {
            closeList()
        }

        ImNodes.editorContextSet(listContext)

        // NODES WINDOW

        ImNodes.getStyle().gridSpacing = 99999f // lol
        ImNodes.pushColorStyle(ImNodesColorStyle.GridBackground, ImColor.floatToColor(0f, 0f, 0f, 0f))

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
                                ImNodes.getNodeDimensions(node.id, nodeSize)

                                table.add(node.id, nodeSize)
                            } else {
                                val pos = table.getPos(node.id)!!
                                ImNodes.setNodeScreenSpacePos(node.id, pos.x, pos.y)
                            }
                        }

                        var titleColor = 0

                        if(isHoverManuallyDetected && hoveredNode == node.id) {
                            if(node is DrawNode<*>) {
                                titleColor = node.titleColor
                                node.titleColor = node.titleHoverColor
                            } else {
                                ImNodes.pushColorStyle(ImNodesColorStyle.TitleBar, EasyVision.imnodesStyle.titleBarHovered)
                            }

                            ImNodes.pushColorStyle(ImNodesColorStyle.NodeBackground, EasyVision.imnodesStyle.nodeBackgroundHovered)
                        }

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

        ImNodes.editorResetPanning(0f, 0f)

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

        ImNodes.getStyle().gridSpacing = 32f // back to normal
        ImNodes.popColorStyle()

        floatingButton.focus = isNodesListOpen && !isHoveringScrollBar

        headers.size = size

        handleClick(!headers.isHeaderHovered)
    }

    private fun handleClick(closeOnClick: Boolean) {
        if(ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            if(hoveredNode >= 0) {
                val instance = easyVision.nodeEditor.addNode(
                    listNodes[hoveredNode]!!::class.java
                ) // add node with the class by using reflection

                if(instance is DrawNode<*>) {
                    val nodePos = ImVec2()
                    ImNodes.getNodeScreenSpacePos(hoveredNode, nodePos)

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
                listContext.destroy()
            }
            listContext = ImNodesContext()

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

        for((category, nodeClasses) in NodeScanner.waitAsyncScan()) {
            val list = mutableListOf<Node<*>>()

            for(nodeClass in nodeClasses) {
                val instance = instantiateNode(nodeClass)

                if(instance is DrawNode && !instance.annotationData.showInList) {
                    continue
                }

                instance.nodesIdContainer = listNodes
                instance.attributesIdContainer = listAttributes
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
        fontManager: FontManager
    ) : Window() {

        override var title = "floating"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove
        )

        private var lastButton = false
        private val hoveringPlusTime = ElapsedTime()

        val onPressed = EventHandler("FloatingButton-OnPressed")

        val buttonFont = fontManager.makeFont("/fonts/icons/Open-Close.ttf", plusFontSize)

        override fun preDrawContents() {
            position = ImVec2(
                window.size.x - plusFontSize * 1.8f, window.size.y - plusFontSize * 1.8f
            )
        }

        override fun drawContents() {
            focus = false

            ImGui.pushFont(buttonFont.imfont)
            val buttonSize = ImGui.getFrameHeight()

            val button = ImGui.button(if(nodeList.isNodesListOpen) "x" else "+", buttonSize, buttonSize)
            ImGui.popFont()

            if(ImGui.isItemHovered()) {
                if(hoveringPlusTime.millis > 500) {
                    val tooltipText = if(!NodeScanner.hasFinishedAsyncScan)
                        "mis_searchingnodes_pleasewait"
                    else if(nodeList.isNodesListOpen) "mis_nodeslist_close" else "mis_nodeslist_open"

                    ImGui.beginTooltip()
                        ImGui.text(tr(tooltipText))
                    ImGui.endTooltip()
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

                    val isOpen = ImGui.collapsingHeader(
                        tr(category.properName), ImGuiTreeNodeFlags.DefaultOpen
                    )
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