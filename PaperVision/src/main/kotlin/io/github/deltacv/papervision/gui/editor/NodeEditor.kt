/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui.editor

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesMiniMapLocation
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.action.editor.CreateNodesAction
import io.github.deltacv.papervision.action.editor.DeleteLinksAction
import io.github.deltacv.papervision.action.editor.DeleteNodesAction
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.gui.display.ImageDisplay
import io.github.deltacv.papervision.gui.display.ImageDisplayNode
import io.github.deltacv.papervision.gui.display.ImageDisplayWindow
import io.github.deltacv.papervision.gui.TooltipPopup
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.menu.RightClickMenuPopup
import io.github.deltacv.papervision.gui.editor.button.OptionsButtonWindow
import io.github.deltacv.papervision.gui.editor.button.PlayButtonWindow
import io.github.deltacv.papervision.gui.editor.button.SourceCodeExportButtonWindow
import io.github.deltacv.papervision.gui.isModalWindowOpen
import io.github.deltacv.papervision.id.DrawableIdElement
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.FlagsNode
import io.github.deltacv.papervision.node.InvisibleNode
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.PaperVisionSerializer
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.flags
import io.github.deltacv.papervision.util.loggerForThis

data class Option(
    val description: String,
    val action: () -> Unit
)

class NodeEditor(val paperVision: PaperVision, private val keyManager: KeyManager) : Window() {

    companion object {
        private const val KEY_PAN_CONSTANT = 5f
        private const val PAN_CONSTANT = 25f
        private const val PASTE_COUNT_OFFSET = 50f
        private const val RIGHT_CLICK_POPUP_THRESHOLD_MS = 200L
        private const val LINK_DELETE_COOLDOWN_MS = 200L
        private const val SCROLL_COOLDOWN_MS = 500L
        private const val MINIMAP_SCALE = 0.15f
    }

    // Core editor state
    var context = ImNodes.editorContextCreate()!!
        private set

    var isNodeFocused = false
        private set

    // Node references
    private val winSizeSupplier: () -> ImVec2 = { paperVision.window.size }
    val originNode by lazy { InvisibleNode() }
    lateinit var flagsNode: FlagsNode

    val flags get() = flagsNode.flags
    val numFlags get() = flagsNode.numFlags

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

    // UI components
    val options = mutableMapOf<String, Option>()
    lateinit var optionsButton: OptionsButtonWindow
        private set
    lateinit var playButton: PlayButtonWindow
        private set
    lateinit var sourceCodeExportButton: SourceCodeExportButtonWindow
        private set
    // Panning state
    val editorPanning = ImVec2(0f, 0f)
    val editorPanningDelta = ImVec2(0f, 0f)
    private val prevEditorPanning = ImVec2(0f, 0f)
    private var prevMouseX = 0f
    private var prevMouseY = 0f

    // Interaction state
    private var rightClickedWhileHoveringNode = false
    private val scrollTimer = ElapsedTime()
    private val rightClickMenuPopupTimer = ElapsedTime()
    private val justDeletedLinkTimer = ElapsedTime()

    // Clipboard state
    private var pasteCount = 0
    private var pasteInitialMousePos = ImVec2()
    private var cutting = false
    var clipboard: String? = null

    // Events
    val onEditorChange = PaperEventHandler("NodeEditor-OnChange")
    val onEditorPan = PaperEventHandler("NodeEditor-OnPan")

    val logger by loggerForThis()

    // Window configuration
    override var title = "editor"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoBringToFrontOnFocus,
        ImGuiWindowFlags.NoTitleBar, ImGuiWindowFlags.NoDecoration
    )

    // Popup state
    private val popupSelection = mutableListOf<DrawableIdElement>()
    private var currentRightClickMenuPopup: RightClickMenuPopup? = null

    // Display
    val outputImageDisplay by lazy { ImageDisplay(paperVision.previzManager.stream) }

    // Shortcuts to PaperVision collections
    val nodes get() = paperVision.nodes
    val attributes get() = paperVision.attributes
    val links get() = paperVision.links
    val keys get() = keyManager.keys

    override fun onEnable() {
        ImNodes.createContext()
        initializeNodes()
        initializeButtons()
        registerOptions()
        registerShortcuts()
        setupPrevizHandlers()
        restoreEditorPanning()
    }

    private fun initializeNodes() {
        originNode.enable()

        if (!::flagsNode.isInitialized) {
            flagsNode = FlagsNode()
        }
        flagsNode.enable()

        inputNode.enable()

        outputNode.streamId = outputImageDisplay.id
        outputNode.enable()
    }

    private fun initializeButtons() {
        sourceCodeExportButton = SourceCodeExportButtonWindow(
            { paperVision.nodeList.floatingButton },
            { size },
            paperVision
        )
        sourceCodeExportButton.enable()

        playButton = PlayButtonWindow(
            sourceCodeExportButton,
            paperVision
        )
        playButton.enable()

        optionsButton = OptionsButtonWindow(
            playButton,
            paperVision,
            options,
        )
        optionsButton.enable()
    }

    private fun registerOptions() {
        options[FontAwesomeIcons.InfoCircle] = Option("mis_about") {
            AboutModalWindow().enable()
        }

        options[FontAwesomeIcons.EarthAmericas] = Option("mis_changelanguage") {
            paperVision.showWelcome(askLanguage = true)
        }
    }

    private fun registerShortcuts() {
        with(keyManager) {
            addShortcut(keys.NativeLeftSuper, keys.Z, ::undo)
            addShortcut(keys.NativeRightSuper, keys.Z, ::undo)
            addShortcut(keys.NativeLeftSuper, keys.Y, ::redo)
            addShortcut(keys.NativeRightSuper, keys.Y, ::redo)
            addShortcut(keys.NativeLeftSuper, keys.X, ::cut)
            addShortcut(keys.NativeRightSuper, keys.X, ::cut)
            addShortcut(keys.NativeLeftSuper, keys.C, ::copy)
            addShortcut(keys.NativeRightSuper, keys.C, ::copy)
            addShortcut(keys.NativeLeftSuper, keys.V, ::paste)
            addShortcut(keys.NativeRightSuper, keys.V, ::paste)
        }
    }

    private fun setupPrevizHandlers() {
        paperVision.previzManager.onStreamChange {
            outputImageDisplay.clientPrevizStream = paperVision.previzManager.stream
        }

        paperVision.previzManager.onPrevizStart {
            val streamWindow = ImageDisplayWindow(outputImageDisplay)
            streamWindow.isCloseable = false
            streamWindow.enable()

            paperVision.previzManager.onPrevizStop.once {
                streamWindow.delete()
            }
        }
    }

    private fun restoreEditorPanning() {
        if (numFlags.containsKey("editorPanningX") && numFlags.containsKey("editorPanningY")) {
            editorPanning.x = numFlags["editorPanningX"]!!.toFloat()
            editorPanning.y = numFlags["editorPanningY"]!!.toFloat()
            logger.info("Restored editor panning from flags to $editorPanning")
        }
    }

    override fun drawContents() {
        ImNodes.editorContextSet(context)

        if (!flagsNode.isEnabled) {
            flagsNode.enable()
        }

        ImNodes.beginNodeEditor()
        ImNodes.setNodeGridSpacePos(originNode.id, 0f, 0f)
        ImNodes.miniMap(MINIMAP_SCALE, ImNodesMiniMapLocation.BottomLeft)

        drawNodesAndLinks()

        ImNodes.endNodeEditor()

        updateEditorState()

        if (Window.isModalWindowOpen || paperVision.nodeList.isNodesListOpen) {
            ImNodes.clearLinkSelection()
            ImNodes.clearNodeSelection()
        } else {
            handleInteractions()
        }

        updatePanning()
        updateRightClickMenuSelection()
        handleDeleteLink()
        handleCreateLink()
        handleDeleteSelection()
    }

    private fun drawNodesAndLinks() {
        for (node in nodes.inmutable) {
            node.editor = this
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
    }

    private fun updateEditorState() {
        isNodeFocused = ImNodes.getHoveredNode() >= 0
    }

    private fun handleInteractions() {
        handleRightClickState()
        handleMouseClickPanning()
        handleKeyboardPanning()
        handleRightClickMenu()
    }

    private fun handleRightClickState() {
        val isFreeToMove = (!isNodeFocused || scrollTimer.millis <= SCROLL_COOLDOWN_MS)

        if (rightClickedWhileHoveringNode) {
            if (ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
                rightClickedWhileHoveringNode = false
            }
        } else {
            rightClickedWhileHoveringNode =
                ImGui.isMouseClicked(ImGuiMouseButton.Right) && !isFreeToMove
        }

        if (!ImGui.isMouseDown(ImGuiMouseButton.Right) &&
            !ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            rightClickMenuPopupTimer.reset()
        }
    }

    private fun handleRightClickMenu() {
        if (ImGui.isMouseReleased(ImGuiMouseButton.Right) &&
            rightClickMenuPopupTimer.millis <= RIGHT_CLICK_POPUP_THRESHOLD_MS &&
            justDeletedLinkTimer.millis >= LINK_DELETE_COOLDOWN_MS) {

            currentRightClickMenuPopup = RightClickMenuPopup(
                paperVision.nodeList,
                ::undo, ::redo, ::cut, ::copy, ::paste,
                popupSelection
            ).apply { enable() }

            logger.debug("Opening right click menu popup")
        }
    }

    private fun handleMouseClickPanning() {
        val shouldPan = ImGui.isMouseDown(ImGuiMouseButton.Middle) ||
                (ImGui.isMouseDown(ImGuiMouseButton.Right) &&
                        rightClickMenuPopupTimer.millis >= 100 &&
                        (!rightClickedWhileHoveringNode || keyManager.pressing(keys.LeftControl)))

        if (shouldPan) {
            editorPanning.x += (ImGui.getMousePosX() - prevMouseX)
            editorPanning.y += (ImGui.getMousePosY() - prevMouseY)
        }

        prevMouseX = ImGui.getMousePosX()
        prevMouseY = ImGui.getMousePosY()
    }

    private fun handleKeyboardPanning() {
        val isFreeToMove = !isNodeFocused || scrollTimer.millis <= SCROLL_COOLDOWN_MS
        if (!isFreeToMove) return

        var doingKeys = false

        // Arrow key panning
        if (keyManager.pressing(keys.ArrowLeft)) {
            editorPanning.x += KEY_PAN_CONSTANT
            doingKeys = true
        } else if (keyManager.pressing(keys.ArrowRight)) {
            editorPanning.x -= KEY_PAN_CONSTANT
            doingKeys = true
        }

        if (keyManager.pressing(keys.ArrowUp)) {
            editorPanning.y += KEY_PAN_CONSTANT
            doingKeys = true
        } else if (keyManager.pressing(keys.ArrowDown)) {
            editorPanning.y -= KEY_PAN_CONSTANT
            doingKeys = true
        }

        if (doingKeys) {
            scrollTimer.reset()
        } else {
            handleMouseWheelPanning()
        }
    }

    private fun handleMouseWheelPanning() {
        val plusPan = ImGui.getIO().mouseWheel * PAN_CONSTANT
        val plusPanX = ImGui.getIO().mouseWheelH * PAN_CONSTANT

        if (plusPan != 0f || plusPanX != 0f) {
            scrollTimer.reset()
        }

        if (keyManager.pressing(keys.LeftShift) || keyManager.pressing(keys.RightShift)) {
            editorPanning.x += plusPan
            editorPanning.y += plusPanX
        } else {
            editorPanning.y += plusPan
            editorPanning.x += plusPanX
        }
    }

    private fun updatePanning() {
        if (editorPanning.x != prevEditorPanning.x || editorPanning.y != prevEditorPanning.y) {
            ImNodes.editorContextResetPanning(editorPanning.x, editorPanning.y)
        } else {
            ImNodes.editorContextGetPanning(editorPanning)
        }

        // Store panning values in flags for serialization
        numFlags["editorPanningX"] = editorPanning.x.toDouble()
        numFlags["editorPanningY"] = editorPanning.y.toDouble()

        editorPanningDelta.x = editorPanning.x - prevEditorPanning.x
        editorPanningDelta.y = editorPanning.y - prevEditorPanning.y

        if (editorPanningDelta.x != 0f || editorPanningDelta.y != 0f) {
            onEditorPan.run()
        }

        prevEditorPanning.x = editorPanning.x
        prevEditorPanning.y = editorPanning.y
    }

    fun undo() {
        logger.info("undo | stack; size: ${paperVision.actions.size}, pointer: ${paperVision.actions.stackPointer}, peek: ${paperVision.actions.peek()}")
        paperVision.actions.peekAndPushback()?.undo()
        pasteCount = 0
    }

    fun redo() {
        logger.info("redo | stack; size: ${paperVision.actions.size}, pointer: ${paperVision.actions.stackPointer}, peek: ${paperVision.actions.peek()}")
        paperVision.actions.pushforwardIfNonNull()
        paperVision.actions.peek()?.execute()
    }

    fun cut(overrideSelection: List<Node<*>>? = null) {
        val selectedNodesList = getSelectedNodesList(overrideSelection) ?: return

        cutting = true
        copy(overrideSelection)

        DeleteNodesAction(selectedNodesList).enable()
    }

    fun copy(overrideSelection: List<Node<*>>? = null) {
        val selectedNodesList = getSelectedNodesList(overrideSelection) ?: return

        pasteCount = 0
        clipboard = PaperVisionSerializer.serialize(selectedNodesList, listOf())

        logger.debug("Clipboard content: $clipboard")
    }

    private fun getSelectedNodesList(overrideSelection: List<Node<*>>?): List<Node<*>>? {
        if (overrideSelection != null) {
            if (overrideSelection.isEmpty()) {
                clipboard = null
                return null
            }
            return overrideSelection
        }

        val selectedNodes = IntArray(ImNodes.numSelectedNodes())
        ImNodes.getSelectedNodes(selectedNodes)

        if (selectedNodes.isEmpty()) {
            clipboard = null
            return null
        }

        return try {
            selectedNodes.map { nodes[it]!! }.filter { it.allowDelete }
        } catch (_: IndexOutOfBoundsException) {
            null
        }
    }

    fun paste() {
        val clipboardContent = clipboard ?: return

        val (nodes, _) = PaperVisionSerializer.deserialize(clipboardContent)

        updatePasteState()
        positionPastedNodes(nodes)
        CreateNodesAction(nodes).enable()

        if (cutting) {
            clipboard = null
            cutting = false
            pasteCount = 0
        } else {
            pasteCount++
        }
    }

    private fun updatePasteState() {
        if (pasteCount == 0) {
            pasteInitialMousePos = ImGui.getMousePos()
        }

        // Reset on mouse movement
        val currentMousePos = ImGui.getMousePos()
        if (pasteInitialMousePos.x != currentMousePos.x ||
            pasteInitialMousePos.y != currentMousePos.y) {
            pasteCount = 0
            pasteInitialMousePos = currentMousePos
        }
    }

    private fun positionPastedNodes(nodes: List<Node<*>>) {
        nodes.forEach { it.forgetSerializedId() }

        if (nodes.size == 1) {
            positionSingleNode(nodes.first())
        } else {
            positionMultipleNodes(nodes)
        }
    }

    private fun positionSingleNode(node: Node<*>) {
        if (node is DrawNode<*>) {
            node.nextNodePosition = ImVec2(
                ImGui.getMousePosX() + pasteCount * PASTE_COUNT_OFFSET,
                ImGui.getMousePosY() + pasteCount * PASTE_COUNT_OFFSET
            )
        }
    }

    private fun positionMultipleNodes(nodes: List<Node<*>>) {
        val centerPos = calculateNodesCenter(nodes) ?: return
        val mousePos = ImGui.getMousePos()

        for (node in nodes) {
            if (node is DrawNode<*>) {
                val offset = node.nextNodePosition?.let {
                    ImVec2(centerPos.x - it.x, centerPos.y - it.y)
                } ?: ImVec2(0f, 0f)

                node.nextNodePosition = ImVec2(
                    offset.x + mousePos.x + pasteCount * PASTE_COUNT_OFFSET,
                    offset.y + mousePos.y + pasteCount * PASTE_COUNT_OFFSET
                )
            }
        }
    }

    private fun calculateNodesCenter(nodes: List<Node<*>>): ImVec2? {
        var totalX = 0f
        var totalY = 0f
        var count = 0

        for (node in nodes) {
            if (node is DrawNode<*>) {
                node.nextNodePosition?.let {
                    totalX += it.x
                    totalY += it.y
                    count++
                }
            }
        }

        return if (count > 0) ImVec2(totalX / count, totalY / count) else null
    }

    fun addNode(nodeClazz: Class<out Node<*>>): Node<*> {
        val instance = Node.instantiateNode(nodeClazz)
            ?: throw IllegalArgumentException(
                "Node $nodeClazz could not be instantiated, is it a valid Node subclass?"
            )

        val action = CreateNodesAction(instance)

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
            window.imageDisplay.clientPrevizStream = paperVision.previzManager.stream
        }

        window.pinToMouse = true
        window.enable()

        attribute.onDelete.once {
            window.delete()
        }

        val link = Link(attribute.id, window.input.id, false, shouldSerialize = false)
        link.enable()

        return window
    }

    private fun updateRightClickMenuSelection() {
        if (currentRightClickMenuPopup?.isVisible == true) return

        popupSelection.clear()

        addSelectedNodesToPopup()
        addSelectedLinksToPopup()
        addHoveredNodeToPopup()
    }

    private fun addSelectedNodesToPopup() {
        val nodeSelection = IntArray(ImNodes.numSelectedNodes())
        ImNodes.getSelectedNodes(nodeSelection)

        for (nodeId in nodeSelection) {
            if (nodeId >= 0) {
                nodes[nodeId]?.let { popupSelection.add(it) }
            }
        }
    }

    private fun addSelectedLinksToPopup() {
        val linkSelection = IntArray(ImNodes.numSelectedLinks())
        ImNodes.getSelectedLinks(linkSelection)

        for (linkId in linkSelection) {
            if (linkId >= 0) {
                links[linkId]?.let { popupSelection.add(it) }
            }
        }
    }

    private fun addHoveredNodeToPopup() {
        val hoveredNodeId = ImNodes.getHoveredNode()
        if (hoveredNodeId >= 0) {
            nodes[hoveredNodeId]?.let { popupSelection.add(it) }
        }
    }

    private val startAttr = ImInt()
    private val endAttr = ImInt()

    private fun handleCreateLink() {
        if (!ImNodes.isLinkCreated(startAttr, endAttr)) return

        val start = startAttr.get()
        val end = endAttr.get()

        val startAttrib = attributes[start]
        val endAttrib = attributes[end]

        // If attributes are null, create link without validation
        if (startAttrib == null || endAttrib == null) {
            CreateLinkAction(Link(start, end)).enable()
            return
        }

        if (!validateLinkCreation(startAttrib, endAttrib)) return

        val input = if (startAttrib.mode == AttributeMode.INPUT) start else end
        val output = if (startAttrib.mode == AttributeMode.OUTPUT) start else end

        createValidatedLink(input, output)
    }

    private fun validateLinkCreation(startAttrib: Attribute, endAttrib: Attribute): Boolean {
        if (startAttrib.mode == endAttrib.mode) {
            return false // Same mode not allowed
        }

        if (!startAttrib.acceptLink(endAttrib) || !endAttrib.acceptLink(startAttrib)) {
            TooltipPopup.showWarning("err_couldntlink_didntmatch")
            return false
        }

        if (startAttrib.parentNode == endAttrib.parentNode) {
            return false // No self-linking
        }

        return true
    }

    private fun createValidatedLink(input: Int, output: Int) {
        val inputAttrib = attributes[input]!!
        val outputAttrib = attributes[output]!!

        // Delete existing input links
        inputAttrib.links.toTypedArray().forEach { it.delete() }

        val link = Link(input, output)
        CreateLinkAction(link).enable()

        if (Node.checkSimpleRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
            TooltipPopup.showWarning("err_couldntlink_recursion")
            link.delete()
        } else {
            paperVision.onUpdate.once {
                link.triggerOnChange()
            }
        }
    }

    private fun handleDeleteLink() {
        val hoveredId = ImNodes.getHoveredLink()

        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && hoveredId >= 0) {
            links[hoveredId]?.delete()
            justDeletedLinkTimer.reset()
        }
    }

    private fun handleDeleteSelection() {
        if (!keyManager.released(keys.Delete)) return

        deleteSelectedNodes()
        deleteSelectedLinks()
    }

    private fun deleteSelectedNodes() {
        if (ImNodes.numSelectedNodes() <= 0) return

        val selectedNodes = IntArray(ImNodes.numSelectedNodes())
        ImNodes.getSelectedNodes(selectedNodes)

        val nodesToDelete = mutableListOf<Node<*>>()

        for (nodeId in selectedNodes) {
            try {
                val node = nodes[nodeId]

                if (node?.joinActionStack == true) {
                    nodesToDelete.add(node)
                } else node?.delete()
            } catch (_: Exception) {
            }
        }

        if (nodesToDelete.isNotEmpty()) {
            DeleteNodesAction(nodesToDelete).enable()
        }
    }

    private fun deleteSelectedLinks() {
        if (ImNodes.numSelectedLinks() <= 0) return

        val selectedLinks = IntArray(ImNodes.numSelectedLinks())
        ImNodes.getSelectedLinks(selectedLinks)

        val linksToDelete = mutableListOf<Link>()

        for (linkId in selectedLinks) {
            links[linkId]?.run {
                if (isDestroyableByUser) {
                    linksToDelete.add(this)
                }
            }
        }

        if (linksToDelete.isNotEmpty()) {
            DeleteLinksAction(linksToDelete).enable()
        }
    }

}
