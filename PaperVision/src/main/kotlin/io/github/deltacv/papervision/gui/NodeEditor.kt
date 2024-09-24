package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.extension.texteditor.TextEditorLanguageDefinition
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.engine.client.message.AskProjectGenClassName
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplay
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplayNode
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplayWindow
import io.github.deltacv.papervision.gui.util.Popup
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.node.InvisibleNode
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.EventHandler
import io.github.deltacv.papervision.util.flags

class NodeEditor(val paperVision: PaperVision, private val keyManager: KeyManager) : Window() {
    companion object {
        val KEY_PAN_CONSTANT = 5f
        val PAN_CONSTANT = 25f
    }

    var context = ImNodesContext()
        private set
    var isNodeFocused = false
        private set

    private val winSizeSupplier: () -> ImVec2 = { paperVision.window.size }

    val originNode by lazy { InvisibleNode() }

    var inputNode = InputMatNode(winSizeSupplier)
        set(value) {
            value.windowSizeSupplier = winSizeSupplier
            field = value
        }

    var outputNode = OutputMatNode(winSizeSupplier)
        set(value) {
            value.windowSizeSupplier = winSizeSupplier
            value.streamId = outputImageDisplay.id
            field = value
        }

    lateinit var playButton: EOCVSimPlayButtonWindow
        private set

    lateinit var sourceCodeExportButton: SourceCodeExportButtonWindow
        private set

    lateinit var eyeFont: Font
        private set

    val editorPanning = ImVec2(0f, 0f)
    val editorPanningDelta = ImVec2(0f, 0f)
    val prevEditorPanning = ImVec2(0f, 0f)

    private var prevMouseX = 0f
    private var prevMouseY = 0f

    private var rightClickedWhileHoveringNode = false

    private val scrollTimer = ElapsedTime()

    val onDraw = EventHandler("NodeEditor-OnDraw")
    val onEditorChange = EventHandler("NodeEditor-OnChange")

    override var title = "editor"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoBringToFrontOnFocus,
        ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoDecoration
    )

    val outputImageDisplay by lazy { ImageDisplay(paperVision.previzManager.stream) }

    val nodes get() = paperVision.nodes
    val attributes get() = paperVision.attributes
    val links get() = paperVision.links

    val Keys get() = keyManager.keys

    override fun onEnable() {
        eyeFont = paperVision.fontManager.makeFont(
            "/fonts/icons/Eye.ttf", 15f
        )

        ImNodes.createContext()

        originNode.enable()
        inputNode.enable()

        outputNode.streamId = outputImageDisplay.id
        outputNode.enable()

        sourceCodeExportButton = SourceCodeExportButtonWindow(
            { paperVision.nodeList.floatingButton },
            { size },
            paperVision
        )

        sourceCodeExportButton.enable()

        playButton = EOCVSimPlayButtonWindow(
            sourceCodeExportButton,
            paperVision,
            paperVision.fontManager
        )

        playButton.enable()

        paperVision.previzManager.onPrevizStart {
            outputImageDisplay.pipelineStream = paperVision.previzManager.stream
            val streamWindow = ImageDisplayWindow(outputImageDisplay)
            streamWindow.enable()

            paperVision.previzManager.onPrevizStop.doOnce {
                streamWindow.delete()
            }
        }
    }

    override fun drawContents() {
        ImNodes.editorContextSet(context)

        onDraw.run()

        ImNodes.beginNodeEditor()

        ImNodes.setNodeGridSpacePos(originNode.id, 0f, 0f)

        // ImNodes.miniMap(0.15f, ImNodesMiniMapLocation.TopLeft)

        for (node in nodes.inmutable) {
            node.editor = this
            node.draw()
            if(node.pollChange()) {
                onEditorChange.run()
            }
        }
        for (link in links) {
            link.draw()
            if(link.pollChange()) {
                onEditorChange.run()
            }
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

        if (paperVision.nodeList.isNodesListOpen) {
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
        val window = ImageDisplayNode(ImageDisplay(paperVision.previzManager.stream))
        paperVision.previzManager.onPrevizStart {
            // automagically update the stream when the previz starts
            window.imageDisplay.pipelineStream = paperVision.previzManager.stream
        }

        window.pinToMouse = true
        window.enable()

        attribute.onDelete.doOnce {
            window.delete()
        }

        val link = Link(attribute.id, window.inputId, false, shouldSerialize = false)
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
                paperVision.onUpdate.doOnce {
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

    class SourceCodeExportButtonWindow(
        val floatingButtonSupplier: () -> NodeList.FloatingButton,
        val nodeEditorSizeSupplier: () -> ImVec2,
        val paperVision: PaperVision
    ) : Window() {
        override var title = ""
        override val windowFlags = flags(
            ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.AlwaysAutoResize
        )

        val frameWidth get() = floatingButtonSupplier().frameWidth

        val sourceFont = paperVision.fontManager.makeFont("/fonts/icons/Source.ttf", NodeList.plusFontSize * 0.65f)

        override fun preDrawContents() {
            position = ImVec2(
                floatingButtonSupplier().position.x - NodeList.plusFontSize * 1.5f,
                floatingButtonSupplier().position.y,
            )
        }

        override fun drawContents() {
            ImGui.pushFont(sourceFont.imfont)

            if(ImGui.button("S", floatingButtonSupplier().frameWidth, floatingButtonSupplier().frameWidth)) {
                paperVision.engineClient.sendMessage(AskProjectGenClassName().onResponseWith<StringResponse> { response ->
                    paperVision.onUpdate.doOnce {
                        CodeDisplayWindow(
                            paperVision.codeGenManager.build(response.value, JavaLanguage),
                            TextEditorLanguageDefinition.cPlusPlus()
                        ).apply {
                            enable()
                            size = ImVec2(nodeEditorSizeSupplier().x * 0.8f, nodeEditorSizeSupplier().y * 0.8f)
                        }
                    }
                })
            }

            ImGui.popFont()
        }
    }

    class EOCVSimPlayButtonWindow(
        val sourceCodeExportButton: SourceCodeExportButtonWindow,
        val paperVision: PaperVision,
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
            val floatingButton = sourceCodeExportButton

            position = ImVec2(
                floatingButton.position.x - NodeList.plusFontSize * 1.5f,
                floatingButton.position.y,
            )
        }

        override fun drawContents() {
            val floatingButton = sourceCodeExportButton

            ImGui.pushFont(playPauseDotsFont.imfont)

            val text = if(paperVision.previzManager.previzRunning) {
                "-"
            } else "+"

            val button = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

            if (lastButton != button && button) {
                if(!paperVision.previzManager.previzRunning) {
                    paperVision.startPrevizAsk()
                } else {
                    paperVision.previzManager.stopPreviz()
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