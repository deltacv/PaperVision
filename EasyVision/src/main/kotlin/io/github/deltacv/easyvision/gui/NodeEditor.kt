package io.github.deltacv.easyvision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.ImNodesContext
import imgui.flag.ImGuiMouseButton
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.gui.util.PopupBuilder
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.node.vision.InputMatNode
import io.github.deltacv.easyvision.node.vision.OutputMatNode
import io.github.deltacv.easyvision.util.ElapsedTime

class NodeEditor(val easyVision: EasyVision, val keyManager: KeyManager) {

    companion object {
        val KEY_PAN_CONSTANT = 5f
        val PAN_CONSTANT = 25f
    }

    val context = ImNodesContext()

    var isNodeFocused = false
        private set

    val inputNode = InputMatNode()
    val outputNode = OutputMatNode()

    fun init() {
        ImNodes.createContext()
        inputNode.enable()
        outputNode.enable()
    }

    val editorPanning = ImVec2(0f, 0f)

    private var prevMouseX = 0f
    private var prevMouseY = 0f

    private val scrollTimer = ElapsedTime()

    fun draw() {
        ImNodes.editorContextSet(context)

        ImNodes.beginNodeEditor()

        for(node in Node.nodes) {
            node.draw()
        }
        for(link in Link.links) {
            link.draw()
        }

        ImNodes.endNodeEditor()

        isNodeFocused = ImNodes.numSelectedNodes() >= 0

        if(easyVision.nodeList.isNodesListOpen) {
            ImNodes.clearLinkSelection()
            ImNodes.clearNodeSelection()
        } else if(isNodeFocused || scrollTimer.millis <= 500) { // not hovering any node
            var doingKeys = false

            // scrolling
            if(keyManager.pressing(Keys.ArrowLeft)) {
                editorPanning.x += KEY_PAN_CONSTANT
                doingKeys = true
            }
            if(keyManager.pressing(Keys.ArrowRight)) {
                editorPanning.x -= KEY_PAN_CONSTANT
                doingKeys = true
            }
            if(keyManager.pressing(Keys.ArrowUp)) {
                editorPanning.y += KEY_PAN_CONSTANT
                doingKeys = true
            }
            if(keyManager.pressing(Keys.ArrowDown)) {
                editorPanning.y -= KEY_PAN_CONSTANT
                doingKeys = true
            }

            if(doingKeys) {
                scrollTimer.reset()
            } else {
                if (ImGui.isMouseDown(ImGuiMouseButton.Middle)) {
                    editorPanning.x += (ImGui.getMousePosX() - prevMouseX)
                    editorPanning.y += (ImGui.getMousePosY() - prevMouseY)
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

            prevMouseX = ImGui.getMousePosX()
            prevMouseY = ImGui.getMousePosY()

            ImNodes.editorResetPanning(editorPanning.x, editorPanning.y)
        }

        handleDeleteLink()
        handleCreateLink()
        handleDeleteSelection()
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
                PopupBuilder.addWarningToolTip("Couldn't link nodes: Types didn't match")
                return // one or both of the attributes didn't accept the link, abort.
            }

            if(startAttrib.parentNode == endAttrib.parentNode) {
                return // we can't link a node to itself!
            }

            inputAttrib.links.toTypedArray().forEach {
                it.delete() // delete the existing link(s) of the input attribute if there's any
            }

            val link = Link(start, end).enable() // create the link and enable it

            if(Node.checkRecursion(inputAttrib.parentNode, outputAttrib.parentNode)) {
                PopupBuilder.addWarningToolTip("Couldn't link nodes: Recursion problem detected")
                // remove the link if a recursion case was detected (e.g both nodes were attached to each other)
                link.delete()
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