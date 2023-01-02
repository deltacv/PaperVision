package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.util.eocvsim.EOCVSimPrevizState.*
import io.github.deltacv.easyvision.gui.eocvsim.ImageDisplayNode
import io.github.deltacv.easyvision.gui.eocvsim.InputSourcesWindow
import io.github.deltacv.easyvision.gui.util.FrameWidthWindow
import io.github.deltacv.easyvision.gui.util.Popup
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
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.easyvision.util.eocvsim.EOCVSimIpcManager
import io.github.deltacv.easyvision.util.event.EventHandler
import io.github.deltacv.easyvision.util.flags

class NodeEditor(val easyVision: EasyVision, val keyManager: KeyManager) : Window() {
    companion object {
        val KEY_PAN_CONSTANT = 5f
        val PAN_CONSTANT = 25f
    }

    val context = ImNodesContext()
    var isNodeFocused = false
        private set

    private val winSizeSupplier: () -> ImVec2 = { easyVision.window.size }

    val originNode by lazy { InvisibleNode() }

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

    lateinit var playButton: EOCVSimPlayButtonWindow
        private set
    lateinit var inputSourcesWindow: InputSourcesWindow
        private set

    lateinit var eyeFont: Font
        private set

    lateinit var pipelineStream: PipelineStream
        private set

    val editorPanning = ImVec2(0f, 0f)
    val editorPanningDelta = ImVec2(0f, 0f)
    val prevEditorPanning = ImVec2(0f, 0f)

    private var prevMouseX = 0f
    private var prevMouseY = 0f

    private var rightClickedWhileHoveringNode = false

    private val scrollTimer = ElapsedTime()

    val onDraw = EventHandler("NodeEditor-OnDraw")

    override var title = "editor"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoBringToFrontOnFocus,
        ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoDecoration
    )

    val nodes get() = easyVision.nodes
    val attributes get() = easyVision.attributes
    val links get() = easyVision.links

    override fun onEnable() {
        eyeFont = easyVision.fontManager.makeFont(
            "/fonts/icons/Eye.ttf", 15f
        )

        pipelineStream = PipelineStream(
            easyVision, offlineImages = arrayOf(
                bufferedImageFromResource("/img/TechnicalDifficulties.png"),
                bufferedImageFromResource("/img/PleaseHangOn.png")
            )
        )

        ImNodes.createContext()

        inputNode.enable()
        outputNode.enable()
        originNode.enable()

        playButton = EOCVSimPlayButtonWindow(
            { easyVision.nodeList.floatingButton },
            easyVision.eocvSimIpc,
            easyVision.nodeEditor.pipelineStream,
            easyVision.fontManager
        )
        playButton.enable()

        inputSourcesWindow = InputSourcesWindow(easyVision.fontManager)
        inputSourcesWindow.enable()
        inputSourcesWindow.attachToIpc(easyVision.eocvSimIpc.ipcClient)
    }

    override fun drawContents() {
        onDraw.run()

        ImNodes.editorContextSet(context)
        ImNodes.beginNodeEditor()

        ImNodes.setNodeGridSpacePos(originNode.id, 0f, 0f)

        // ImNodes.miniMap(0.15f, ImNodesMiniMapLocation.TopLeft)

        for (node in nodes.inmutable) {
            node.eocvSimIpc = easyVision.eocvSimIpc
            node.editor = this
            node.draw()
        }
        for (link in links) {
            link.draw()
        }

        ImNodes.endNodeEditor()

        isNodeFocused = ImNodes.getHoveredNode() >= 0

        val isFreeToMove = (!isNodeFocused || scrollTimer.millis <= 500)

        if(rightClickedWhileHoveringNode) {
            if(ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
                rightClickedWhileHoveringNode = false
            }
        } else {
            rightClickedWhileHoveringNode = ImGui.isMouseClicked(ImGuiMouseButton.Right) && !isFreeToMove
        }

        if (easyVision.nodeList.isNodesListOpen) {
            ImNodes.clearLinkSelection()
            ImNodes.clearNodeSelection()
        } else if (
            ImGui.isMouseDown(ImGuiMouseButton.Middle)
            || (ImGui.isMouseDown(ImGuiMouseButton.Right) && (!rightClickedWhileHoveringNode || keyManager.pressing(Keys.LeftControl)))
        ) {
            editorPanning.x += (ImGui.getMousePosX() - prevMouseX)
            editorPanning.y += (ImGui.getMousePosY() - prevMouseY)
        } else if(isFreeToMove) { // not hovering any node
            var doingKeys = false

            // scrolling
            if (keyManager.pressing(Keys.ArrowLeft)) {
                editorPanning.x += KEY_PAN_CONSTANT
                doingKeys = true
            } else if (keyManager.pressing(Keys.ArrowRight)) {
                editorPanning.x -= KEY_PAN_CONSTANT
                doingKeys = true
            }

            if (keyManager.pressing(Keys.ArrowUp)) {
                editorPanning.y += KEY_PAN_CONSTANT
                doingKeys = true
            } else if (keyManager.pressing(Keys.ArrowDown)) {
                editorPanning.y -= KEY_PAN_CONSTANT
                doingKeys = true
            }

            if (doingKeys) {
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
        }

        if (editorPanning.x != prevEditorPanning.x || editorPanning.y != prevEditorPanning.y) {
            ImNodes.editorResetPanning(editorPanning.x, editorPanning.y)
        } else {
            ImNodes.editorContextGetPanning(editorPanning)
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
    }

    fun addNode(nodeClazz: Class<out Node<*>>): Node<*> {
        val instance = instantiateNode(nodeClazz)

        instance.enable()
        return instance
    }

    fun startImageDisplayFor(attribute: Attribute): ImageDisplayNode {
        val window = ImageDisplayNode(pipelineStream)
        window.pinToMouse = true
        window.enable()

        attribute.onDelete.doOnce {
            window.delete()
        }

        val link = Link(attribute.id, window.inputId, false)
        link.enable()

        window.onDelete.doOnce { link.delete() }

        return window
    }

    private val startAttr = ImInt()
    private val endAttr = ImInt()

    private fun handleCreateLink() {
        if (ImNodes.isLinkCreated(startAttr, endAttr)) {
            val start = startAttr.get()
            val end = endAttr.get()

            val startAttrib = attributes[start]
            val endAttrib = attributes[end]

            // one of the attributes was null so we can't perform additional checks to ensure stuff
            // we will just go ahead and create the link hoping nothing breaks lol
            if (startAttrib == null || endAttrib == null) {
                Link(start, end).enable() // create the link and enable it
                return
            }

            val input = if (startAttrib.mode == AttributeMode.INPUT) start else end
            val output = if (startAttrib.mode == AttributeMode.OUTPUT) start else end

            val inputAttrib = attributes[input]!!
            val outputAttrib = attributes[output]!!

            if (startAttrib.mode == endAttrib.mode) {
                return // linked attributes cannot be of the same mode
            }

            if (!startAttrib.acceptLink(endAttrib) || !endAttrib.acceptLink(startAttrib)) {
                Popup.warning("err_couldntlink_didntmatch")
                return // one or both of the attributes didn't accept the link, abort.
            }

            if (startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            inputAttrib.links.toTypedArray().forEach {
                it.delete() // delete the existing link(s) of the input attribute if there's any
            }

            val link = Link(start, end)
            link.enable() // create the link and enable it

            if (Node.checkRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
                Popup.warning("err_couldntlink_recursion")
                // remove the link if a recursion case was detected (e.g both nodes were attached to each other already)
                link.delete()
            } else {
                easyVision.onUpdate.doOnce {
                    link.triggerOnChange()
                }
            }
        }
    }

    private fun handleDeleteLink() {
        val hoveredId = ImNodes.getHoveredLink()

        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && hoveredId >= 0) {
            val hoveredLink = links[hoveredId]
            hoveredLink?.delete()
        }
    }

    private fun handleDeleteSelection() {
        if (keyManager.released(Keys.Delete)) {
            if (ImNodes.numSelectedNodes() > 0) {
                val selectedNodes = IntArray(ImNodes.numSelectedNodes())
                ImNodes.getSelectedNodes(selectedNodes)

                for (node in selectedNodes) {
                    try {
                        nodes[node]?.delete()
                    } catch (ignored: IndexOutOfBoundsException) {
                    }
                }
            }

            if (ImNodes.numSelectedLinks() > 0) {
                val selectedLinks = IntArray(ImNodes.numSelectedLinks())
                ImNodes.getSelectedLinks(selectedLinks)

                for (link in selectedLinks) {
                    links[link]?.run {
                        if(isDestroyableByUser)
                            delete()
                    }
                }
            }
        }
    }

    fun destroy() {
        ImNodes.destroyContext()
    }

    class EOCVSimPlayButtonWindow(
        val floatingButtonSupplier: () -> FrameWidthWindow,
        val eocvSimIpc: EOCVSimIpcManager,
        val stream: PipelineStream,
        fontManager: FontManager
    ) : Window() {

        override var title = "eocv sim control"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.AlwaysAutoResize
        )

        private var lastButton = false

        val playPauseDotsFont = fontManager.makeFont("/fonts/icons/Play-Pause-Dots.ttf", NodeList.plusFontSize * 0.65f)

        override fun preDrawContents() {
            val floatingButton = floatingButtonSupplier()

            position = ImVec2(
                floatingButton.position.x - NodeList.plusFontSize * 1.5f,
                floatingButton.position.y,
            )
        }

        override fun drawContents() {
            val floatingButton = floatingButtonSupplier()

            ImGui.pushFont(playPauseDotsFont.imfont)

            val text = when (eocvSimIpc.previzState) {
                RUNNING -> "-"
                RUNNING_BUT_NOT_CONNECTED -> "."
                else -> "+"
            }

            val button = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

            if (lastButton != button && button) {
                if (eocvSimIpc.previzState == RUNNING || eocvSimIpc.previzState == RUNNING_BUT_NOT_CONNECTED) {
                    eocvSimIpc.stopPrevizSession()
                    stream.stop()
                } else {
                    eocvSimIpc.startPrevizSession("e")

                    if (ImageDisplayNode.displayWindows.elements.isNotEmpty()) {
                        stream.startIfNeeded()
                    }
                }
            }

            ImGui.popFont()

            lastButton = button
        }

    }

}

fun instantiateNode(nodeClazz: Class<out Node<*>>) = try {
    nodeClazz.getConstructor().newInstance()
} catch (e: NoSuchMethodException) {
    throw UnsupportedOperationException(
        "Node ${nodeClazz.typeName} does not implement a constructor with no parameters",
        e
    )
} catch (e: IllegalStateException) {
    throw UnsupportedOperationException("Error while instantiating node ${nodeClazz.typeName}", e)
}