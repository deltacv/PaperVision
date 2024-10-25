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

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesMiniMapLocation
import imgui.extension.texteditor.TextEditorLanguageDefinition
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.action.editor.CreateNodeAction
import io.github.deltacv.papervision.action.editor.DeleteLinksAction
import io.github.deltacv.papervision.action.editor.DeleteNodesAction
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.engine.client.message.AskProjectGenClassNameMessage
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplay
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplayNode
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplayWindow
import io.github.deltacv.papervision.gui.util.Popup
import io.github.deltacv.papervision.gui.util.Tooltip
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.id.DrawableIdElement
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.node.FlagsNode
import io.github.deltacv.papervision.node.InvisibleNode
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags
import io.github.deltacv.papervision.util.loggerForThis

class NodeEditor(val paperVision: PaperVision, private val keyManager: KeyManager) : Window() {
    companion object {
        const val KEY_PAN_CONSTANT = 5f
        const val PAN_CONSTANT = 25f
    }

    var context = ImNodes.editorContextCreate()
        private set
    var isNodeFocused = false
        private set

    private val winSizeSupplier: () -> ImVec2 = { paperVision.window.size }

    val originNode by lazy { InvisibleNode() }
    lateinit var flagsNode: FlagsNode

    val flags get() = flagsNode.flags

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

    val fontAwesome get() = paperVision.fontAwesome

    val editorPanning = ImVec2(0f, 0f)
    val editorPanningDelta = ImVec2(0f, 0f)
    val prevEditorPanning = ImVec2(0f, 0f)

    private var prevMouseX = 0f
    private var prevMouseY = 0f

    private var rightClickedWhileHoveringNode = false

    private val scrollTimer = ElapsedTime()

    val onDraw = PaperVisionEventHandler("NodeEditor-OnDraw")
    val onEditorChange = PaperVisionEventHandler("NodeEditor-OnChange")
    val onEditorPan = PaperVisionEventHandler("NodeEditor-OnPan")

    override var title = "editor"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoBringToFrontOnFocus,
        ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoDecoration
    )

    val rightClickMenuPopup by lazy {
        RightClickMenuPopup(paperVision.nodeList, paperVision::undo, paperVision::redo)
    }

    private val rightClickMenuPopupTimer = ElapsedTime()
    private val justDeletedLinkTimer = ElapsedTime()

    val outputImageDisplay by lazy { ImageDisplay(paperVision.previzManager.stream) }

    val nodes get() = paperVision.nodes
    val attributes get() = paperVision.attributes
    val links get() = paperVision.links

    val Keys get() = keyManager.keys

    override fun onEnable() {
        ImNodes.createContext()

        originNode.enable()

        if(!::flagsNode.isInitialized) {
            flagsNode = FlagsNode()
        }
        flagsNode.enable()

        inputNode.enable()
        rightClickMenuPopup.enable()

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
            paperVision.fontAwesomeBig
        )

        playButton.enable()

        paperVision.previzManager.onStreamChange {
            outputImageDisplay.pipelineStream = paperVision.previzManager.stream
        }

        paperVision.previzManager.onPrevizStart {
            val streamWindow = ImageDisplayWindow(outputImageDisplay)
            streamWindow.enable()

            paperVision.previzManager.onPrevizStop.doOnce {
                streamWindow.delete()
            }
        }
    }

    override fun drawContents() {
        ImNodes.editorContextSet(context)

        if(!flagsNode.isEnabled) {
            flagsNode.enable()
        }

        onDraw.run()

        ImNodes.beginNodeEditor()

        ImNodes.setNodeGridSpacePos(originNode.id, 0f, 0f)

        ImNodes.miniMap(0.15f, ImNodesMiniMapLocation.BottomLeft)

        for (node in nodes.inmutable) {
            node.editor = this
            node.fontAwesome = fontAwesome

            node.draw()
            if (node.pollChange()) {
                onEditorChange.run()
            }
        }
        for (link in links.inmutable) {
            link.draw()
            if (link.pollChange()) {
                onEditorChange.run()
            }
        }

        ImNodes.endNodeEditor()

        isNodeFocused = ImNodes.getHoveredNode() >= 0

        val isFreeToMove = (!isNodeFocused || scrollTimer.millis <= 500)

        if (rightClickedWhileHoveringNode) {
            if (ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
                rightClickedWhileHoveringNode = false
            }
        } else {
            rightClickedWhileHoveringNode = ImGui.isMouseClicked(ImGuiMouseButton.Right) && !isFreeToMove
        }

        if ((!ImGui.isMouseDown(ImGuiMouseButton.Right) && !ImGui.isMouseReleased(ImGuiMouseButton.Right))) {
            rightClickMenuPopupTimer.reset()
        }

        if (ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            if (rightClickMenuPopupTimer.millis <= 200 && (!paperVision.nodeList.isNodesListOpen && !paperVision.isModalWindowOpen && justDeletedLinkTimer.millis >= 200)) {
                rightClickMenuPopup.open()
            }
        }

        if (paperVision.nodeList.isNodesListOpen || paperVision.isModalWindowOpen) {
            ImNodes.clearLinkSelection()
            ImNodes.clearNodeSelection()
        } else if (
            ImGui.isMouseDown(ImGuiMouseButton.Middle) ||
            (ImGui.isMouseDown(ImGuiMouseButton.Right)
                    && rightClickMenuPopupTimer.millis >= 100 && (!rightClickedWhileHoveringNode || keyManager.pressing(Keys.LeftControl)))
        ) {
            editorPanning.x += (ImGui.getMousePosX() - prevMouseX)
            editorPanning.y += (ImGui.getMousePosY() - prevMouseY)
        } else if (isFreeToMove) { // not hovering any node
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
                val plusPanX = ImGui.getIO().mouseWheelH * PAN_CONSTANT

                if (plusPan != 0f) {
                    scrollTimer.reset()
                }

                if (keyManager.pressing(Keys.LeftShift) || keyManager.pressing(Keys.RightShift)) {
                    editorPanning.x += plusPan
                    editorPanning.y += plusPanX
                } else {
                    editorPanning.y += plusPan
                    editorPanning.x += plusPanX
                }
            }
        }

        if (editorPanning.x != prevEditorPanning.x || editorPanning.y != prevEditorPanning.y) {
            ImNodes.editorContextResetPanning(editorPanning.x, editorPanning.y)
        } else {
            ImNodes.editorContextGetPanning(editorPanning)
        }

        editorPanningDelta.x = editorPanning.x - prevEditorPanning.x
        editorPanningDelta.y = editorPanning.y - prevEditorPanning.y

        if(editorPanningDelta.x != 0f || editorPanningDelta.y != 0f) {
            onEditorPan.run()
        }

        prevEditorPanning.x = editorPanning.x
        prevEditorPanning.y = editorPanning.y

        prevMouseX = ImGui.getMousePosX()
        prevMouseY = ImGui.getMousePosY()

        updateRightClickMenuSelection()

        handleDeleteLink()
        handleCreateLink()
        handleDeleteSelection()
    }

    fun addNode(nodeClazz: Class<out Node<*>>): Node<*> {
        val instance = instantiateNode(nodeClazz)
        val action = CreateNodeAction(instance)

        if (instance.joinActionStack) {
            action.enable()
        } else {
            action.execute()
        }

        return instance
    }

    fun startImageDisplayFor(attribute: Attribute): ImageDisplayNode {
        val window = ImageDisplayNode(ImageDisplay(paperVision.previzManager.stream))
        paperVision.previzManager.onStreamChange {
            // automagically update the stream of all windows
            window.imageDisplay.pipelineStream = paperVision.previzManager.stream
        }

        window.pinToMouse = true
        window.enable()

        attribute.onDelete.doOnce {
            window.delete()
        }

        val link = Link(attribute.id, window.input.id, false, shouldSerialize = false)
        link.enable()

        return window
    }

    private fun updateRightClickMenuSelection() {
        if (rightClickMenuPopup.isVisible) {
            return
        }

        rightClickMenuPopup.selection.clear()

        val nodeSelection = IntArray(ImNodes.numSelectedNodes())
        ImNodes.getSelectedNodes(nodeSelection)

        for (node in nodeSelection) {
            if (node < 0) continue

            nodes[node]?.let {
                rightClickMenuPopup.selection.add(it)
            }
        }

        val linkSelection = IntArray(ImNodes.numSelectedLinks())
        ImNodes.getSelectedLinks(linkSelection)

        for (link in linkSelection) {
            if (link < 0) continue

            links[link]?.let {
                rightClickMenuPopup.selection.add(it)
            }
        }

        if (ImNodes.getHoveredNode() >= 0) {
            nodes[ImNodes.getHoveredNode()]?.let {
                rightClickMenuPopup.selection.add(it)
            }
        }
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
                CreateLinkAction(Link(start, end)).enable() // create the link and enable it
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
                Tooltip.warning("err_couldntlink_didntmatch")
                return // one or both of the attributes didn't accept the link, abort.
            }

            if (startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            inputAttrib.links.toTypedArray().forEach {
                it.delete() // delete the existing link(s) of the input attribute if there's any
            }

            val link = Link(start, end)
            CreateLinkAction(link).enable()

            if (Node.checkRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
                Tooltip.warning("err_couldntlink_recursion")
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
            justDeletedLinkTimer.reset()
        }
    }

    private fun handleDeleteSelection() {
        if (keyManager.released(Keys.Delete)) {
            if (ImNodes.numSelectedNodes() > 0) {
                val selectedNodes = IntArray(ImNodes.numSelectedNodes())
                ImNodes.getSelectedNodes(selectedNodes)

                val nodesToDelete = mutableListOf<Node<*>>()

                for (node in selectedNodes) {
                    try {
                        val node = nodes[node]!!

                        if (node.joinActionStack) {
                            nodesToDelete.add(node)
                        } else {
                            node.delete()
                        }
                    } catch (_: Exception) {
                    }
                }

                DeleteNodesAction(nodesToDelete).enable()
            }

            if (ImNodes.numSelectedLinks() > 0) {
                val selectedLinks = IntArray(ImNodes.numSelectedLinks())
                ImNodes.getSelectedLinks(selectedLinks)

                val linksToDelete = mutableListOf<Link>()

                for (link in selectedLinks) {
                    links[link]?.run {
                        if (isDestroyableByUser)
                            linksToDelete.add(this)
                    }
                }

                DeleteLinksAction(linksToDelete).enable()
            }
        }
    }

    fun destroy() {
        ImNodes.destroyContext()
    }

    class RightClickMenuPopup(
        val nodeList: NodeList,
        val undo: () -> Unit,
        val redo: () -> Unit
    ) : Popup() {
        var selection = mutableListOf<DrawableIdElement>()

        override val title = "right click menu"
        override val flags = flags(ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove)

        override fun drawContents() {
            ImGui.pushStyleColor(ImGuiCol.Button, 0)

            if (ImGui.button("Undo")) {
                undo()
                ImGui.closeCurrentPopup()
            }

            if (ImGui.button("Redo")) {
                redo()
                ImGui.closeCurrentPopup()
            }

            if (selection.isNotEmpty()) {
                if (ImGui.button("Delete")) {
                    selection.find { it is Node<*> }?.let {
                        val nodesToDelete = selection.filterIsInstance<Node<*>>()
                        DeleteNodesAction(nodesToDelete).enable()
                    }

                    selection.find { it is Link }?.let {
                        val linksToDelete = selection.filterIsInstance<Link>()
                        DeleteLinksAction(linksToDelete).enable()
                    }

                    ImGui.closeCurrentPopup()
                }
            }

            ImGui.separator()

            if (ImGui.button("Add Node")) {
                nodeList.showList()
            }

            ImGui.popStyleColor()
        }

    }

    class SourceCodeExportSelectLanguageWindow(
        val paperVision: PaperVision,
        val nodeEditorSizeSupplier: () -> ImVec2
    ) : Window() {

        companion object {
            val separationMultiplier = 1.5f
        }

        override var title = "$[win_selectlanguage]"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoResize,
            ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.NoCollapse
        )

        override val isModal = true

        val logger by loggerForThis()

        override fun drawContents() {
            ImGui.pushFont(paperVision.fontAwesomeBrandsBig.imfont)
            ImGui.pushStyleColor(ImGuiCol.Button, 0)

            if (ImGui.button(FontAwesomeIcons.Brands.Java)) {
                openSourceCodeWindow(JavaLanguage)
                delete()
            }

            ImGui.sameLine()
            ImGui.indent(ImGui.getItemRectSizeX() * separationMultiplier)

            if (ImGui.button(FontAwesomeIcons.Brands.Python)) {
                openSourceCodeWindow(CPythonLanguage)
                delete()
            }

            ImGui.popFont()
            ImGui.popStyleColor()
        }


        private fun openSourceCodeWindow(language: Language) {
            fun openWindow(code: String?) {
                if (code == null) {
                    logger.warn("Code generation failed, cancelled opening source code window")
                    return
                }

                CodeDisplayWindow(
                    code,
                    TextEditorLanguageDefinition.CPlusPlus(),
                    paperVision.codeFont
                ).apply {
                    enable()
                    size = ImVec2(nodeEditorSizeSupplier().x * 0.8f, nodeEditorSizeSupplier().y * 0.8f)
                }
            }

            if (paperVision.engineClient.bridge.isConnected) {
                paperVision.engineClient.sendMessage(AskProjectGenClassNameMessage().onResponseWith<StringResponse> { response ->
                    paperVision.onUpdate.doOnce {
                        openWindow(paperVision.codeGenManager.build(response.value, language))
                    }
                })
            } else {
                paperVision.onUpdate.doOnce {
                    openWindow(paperVision.codeGenManager.build("Mack", language))
                }
            }
        }
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

        override fun preDrawContents() {
            position = ImVec2(
                floatingButtonSupplier().position.x - NodeList.PLUS_FONT_SIZE * 1.7f,
                floatingButtonSupplier().position.y,
            )
        }

        override fun drawContents() {
            ImGui.pushFont(paperVision.fontAwesomeBig.imfont)

            if (ImGui.button(
                    FontAwesomeIcons.FileCode,
                    floatingButtonSupplier().frameWidth,
                    floatingButtonSupplier().frameWidth
                )
            ) {
                SourceCodeExportSelectLanguageWindow(paperVision, nodeEditorSizeSupplier).enable()
            }

            ImGui.popFont()
        }
    }

    class EOCVSimPlayButtonWindow(
        val sourceCodeExportButton: SourceCodeExportButtonWindow,
        val paperVision: PaperVision,
        val fontAwesome: Font
    ) : Window() {

        override var title = "eocv sim control"
        override val windowFlags = flags(
            ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
            ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
            ImGuiWindowFlags.AlwaysAutoResize
        )

        private var lastButton = false

        override fun preDrawContents() {
            val floatingButton = sourceCodeExportButton

            position = ImVec2(
                floatingButton.position.x - NodeList.PLUS_FONT_SIZE * 1.7f,
                floatingButton.position.y,
            )
        }

        override fun drawContents() {
            val floatingButton = sourceCodeExportButton

            ImGui.pushFont(fontAwesome.imfont)

            val text = if (paperVision.previzManager.previzRunning) {
                FontAwesomeIcons.Stop
            } else FontAwesomeIcons.Play

            val button = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

            if (lastButton != button && button) {
                if (!paperVision.previzManager.previzRunning) {
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