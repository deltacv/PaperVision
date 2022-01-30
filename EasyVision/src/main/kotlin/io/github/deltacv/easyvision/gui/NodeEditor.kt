package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.imnodes.flag.ImNodesMiniMapLocation
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.gui.util.PopupBuilder
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.io.PipelineStream
import io.github.deltacv.easyvision.io.bufferedImageFromResource
import io.github.deltacv.easyvision.node.InvisibleNode
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.node.vision.InputMatNode
import io.github.deltacv.easyvision.node.vision.OutputMatNode
import io.github.deltacv.easyvision.platform.PlatformTexture
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.easyvision.util.event.EventHandler
import io.github.deltacv.easyvision.util.flags
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.mai18n.tr

class NodeEditor(val easyVision: EasyVision, val keyManager: KeyManager) : Window() {

    companion object {
        val KEY_PAN_CONSTANT = 5f
        val PAN_CONSTANT = 25f
    }

    val context = ImNodesContext()

    var isNodeFocused = false
        private set

    private val winSizeSupplier: () -> ImVec2 = { easyVision.window.size }

    val originNode = InvisibleNode()

    var inputNode = InputMatNode(winSizeSupplier)
        set(value) {
            value.windowSizeSupplier = winSizeSupplier
            field = value
        }

    var outputNode = OutputMatNode(winSizeSupplier)
        set(value) {
            value.windowSizeSupplier = winSizeSupplier
            field = value
        }

    lateinit var eyeFont: Font
        private set

    lateinit var pipelineStream: PipelineStream
        private set

    val editorPanning = ImVec2(0f, 0f)
    val editorPanningDelta = ImVec2(0f, 0f)
    val prevEditorPanning = ImVec2(0f, 0f)

    private var prevMouseX = 0f
    private var prevMouseY = 0f

    private val scrollTimer = ElapsedTime()

    val onDraw = EventHandler("NodeEditor-OnDraw")


    override var title = "Editor"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoBringToFrontOnFocus,
        ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoDecoration
    )

    override fun onEnable() {
        eyeFont = easyVision.fontManager.makeFont(
            "/fonts/icons/Eye.ttf", "Eye", 15f
        )

        pipelineStream = PipelineStream(easyVision, offlineImages = arrayOf(
            bufferedImageFromResource("/img/TechnicalDifficulties.png"),
            bufferedImageFromResource("/img/PleaseHangOn.png")
        ))

        ImNodes.createContext()

        inputNode.enable()
        outputNode.enable()
        originNode.enable()
    }

    override fun drawContents() {
        onDraw.run()

        ImNodes.editorContextSet(context)

        ImNodes.beginNodeEditor()

        ImNodes.setNodeGridSpacePos(originNode.id, 0f, 0f)

        ImNodes.miniMap(0.15f, ImNodesMiniMapLocation.TopLeft)

        for(node in Node.nodes) {
            node.codeGenManager = easyVision.codeGenManager
            node.editor = this
            node.draw()
        }
        for(link in Link.links) {
            link.draw()
        }

        ImNodes.endNodeEditor()

        isNodeFocused = ImNodes.numSelectedNodes() > 0 || ImNodes.getHoveredNode() >= 0

        if(easyVision.nodeList.isNodesListOpen) {
            ImNodes.clearLinkSelection()
            ImNodes.clearNodeSelection()
        } else if(ImGui.isMouseDown(ImGuiMouseButton.Middle)) {
            editorPanning.x += (ImGui.getMousePosX() - prevMouseX)
            editorPanning.y += (ImGui.getMousePosY() - prevMouseY)
        } else if(!isNodeFocused || scrollTimer.millis <= 500) { // not hovering any node
            var doingKeys = false

            // scrolling
            if(keyManager.pressing(Keys.ArrowLeft)) {
                editorPanning.x += KEY_PAN_CONSTANT
                doingKeys = true
            } else if(keyManager.pressing(Keys.ArrowRight)) {
                editorPanning.x -= KEY_PAN_CONSTANT
                doingKeys = true
            }

            if(keyManager.pressing(Keys.ArrowUp)) {
                editorPanning.y += KEY_PAN_CONSTANT
                doingKeys = true
            } else if(keyManager.pressing(Keys.ArrowDown)) {
                editorPanning.y -= KEY_PAN_CONSTANT
                doingKeys = true
            }

            if(doingKeys) {
                scrollTimer.reset()
            } else {
                val plusPan = ImGui.getIO().mouseWheel * PAN_CONSTANT

                if (plusPan != 0f) {
                    scrollTimer.reset()
                }

                if (keyManager.pressing(Keys.LeftShift) || keyManager.pressing(Keys.RightShift)) {
                    editorPanning.x += plusPan
                } else {
                    editorPanning.y += plusPan
                }
            }

            if(editorPanning.x != prevEditorPanning.x || editorPanning.y != prevEditorPanning.y) {
                ImNodes.editorResetPanning(editorPanning.x, editorPanning.y)
            } else {
                ImNodes.getNodeEditorSpacePos(originNode.id, editorPanning)
            }
        }

        editorPanningDelta.x = editorPanning.x - prevEditorPanning.x
        editorPanningDelta.y = editorPanning.y - prevEditorPanning.y

        prevEditorPanning.x = editorPanning.x
        prevEditorPanning.y = editorPanning.y

        prevMouseX = ImGui.getMousePosX()
        prevMouseY = ImGui.getMousePosY()

        handleDeleteLink()
        handleCreateLink()
        handleDeleteSelection()

        for(display in ImageDisplayWindow.displayWindows) {
            display.draw()
        }
    }

    fun addNode(nodeClazz: Class<out Node<*>>): Node<*> {
        val instance = instantiateNode(nodeClazz)

        instance.enable()
        return instance
    }

    fun startImageDisplayFor(attribute: Attribute, title: String): ImageDisplayWindow {
        val window = ImageDisplayWindow(title, pipelineStream)
        window.enable()

        pipelineStream.startIfNeeded()

        attribute.onDelete.doOnce {
            window.delete()
        }

        return window
    }

    private val startAttr = ImInt()
    private val endAttr = ImInt()

    private fun handleCreateLink() {
        if(ImNodes.isLinkCreated(startAttr, endAttr)) {
            val start = startAttr.get()
            val end = endAttr.get()

            val startAttrib = Node.attributes[start]!!
            val endAttrib = Node.attributes[end]!!

            val input  = if(startAttrib.mode == AttributeMode.INPUT) start else end
            val output = if(startAttrib.mode == AttributeMode.OUTPUT) start else end

            val inputAttrib = Node.attributes[input]!!
            val outputAttrib = Node.attributes[output]!!

            if(startAttrib.mode == endAttrib.mode) {
                return // linked attributes cannot be of the same mode
            }

            if(!startAttrib.acceptLink(endAttrib) || !endAttrib.acceptLink(startAttrib)) {
                PopupBuilder.addWarningToolTip(tr("err_couldntlink_didntmatch"))
                return // one or both of the attributes didn't accept the link, abort.
            }

            if(startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            inputAttrib.links.toTypedArray().forEach {
                it.delete() // delete the existing link(s) of the input attribute if there's any
            }

            val link = Link(start, end)
            link.enable() // create the link and enable it

            if(Node.checkRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
                PopupBuilder.addWarningToolTip(tr("err_couldntlink_recursion"))
                // remove the link if a recursion case was detected (e.g both nodes were attached to each other already)
                link.delete()
            } else {
                link.triggerOnChange()
            }
        }
    }

    private fun handleDeleteLink() {
        val hoveredId = ImNodes.getHoveredLink()

        if(ImGui.isMouseClicked(ImGuiMouseButton.Right) && hoveredId >= 0) {
            val hoveredLink = Link.links[hoveredId]
            hoveredLink?.delete()
        }
    }

    private fun handleDeleteSelection() {
        if(keyManager.released(Keys.Delete)) {
            if(ImNodes.numSelectedNodes() > 0) {
                val selectedNodes = IntArray(ImNodes.numSelectedNodes())
                ImNodes.getSelectedNodes(selectedNodes)

                for(node in selectedNodes) {
                    try {
                        Node.nodes[node]?.delete()
                    } catch(ignored: IndexOutOfBoundsException) {}
                }
            }

            if(ImNodes.numSelectedLinks() > 0) {
                val selectedLinks = IntArray(ImNodes.numSelectedLinks())
                ImNodes.getSelectedLinks(selectedLinks)

                for(link in selectedLinks) {
                    Link.links[link]?.delete()
                }
            }
        }
    }

    fun destroy() {
        ImNodes.destroyContext()
    }

}

fun instantiateNode(nodeClazz: Class<out Node<*>>) = try {
    nodeClazz.getConstructor().newInstance()
} catch(e: NoSuchMethodException) {
    throw UnsupportedOperationException("Node ${nodeClazz.typeName} does not implement a constructor with no parameters", e)
} catch(e: IllegalStateException) {
    throw UnsupportedOperationException("Error while instantiating node ${nodeClazz.typeName}", e)
}