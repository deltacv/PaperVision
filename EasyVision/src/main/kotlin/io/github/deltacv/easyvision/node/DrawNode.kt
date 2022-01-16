package io.github.deltacv.easyvision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import imgui.flag.ImGuiMouseButton
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValueMessage
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValuesMessage
import io.github.deltacv.mai18n.tr
import java.lang.IllegalArgumentException
import java.util.*

abstract class DrawNode<S: CodeGenSession>(
    allowDelete: Boolean = true
) : Node<S>(allowDelete) {

    var nextNodePosition: ImVec2? = null

    var pinToMouse = false
    private var lastPinToMouse = false
    private var pinToMouseOffset = ImVec2()

    private var isFirstDraw = true

    val annotationData by lazy {
        val annotation = this.javaClass.getAnnotation(RegisterNode::class.java)
            ?: throw IllegalArgumentException("Node ${javaClass.typeName} needs to have a @RegisterNode annotation")

        AnnotationData(annotation.name, annotation.description, annotation.category, annotation.showInList)
    }

    var titleColor = annotationData.category.color
    var titleHoverColor = annotationData.category.colorSelected

    open fun init() {}

    override fun draw() {
        val title = annotationData.name

        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBar, titleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarHovered, titleHoverColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.TitleBarSelected, titleHoverColor)

        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted(tr(title))
            ImNodes.endNodeTitleBar()

            drawNode()
            drawAttributes()
        ImNodes.endNode()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        if(isFirstDraw) {
            init()
            isFirstDraw = false
        }

        nextNodePosition?.let {
            ImNodes.setNodeScreenSpacePos(id, it.x, it.y)
            nextNodePosition = null
        }

        if(pinToMouse) {
            val mousePos = ImGui.getMousePos()

            if(pinToMouse != lastPinToMouse) {
                val nodeDims = ImVec2()
                ImNodes.getNodeDimensions(id, nodeDims)

                pinToMouseOffset = ImVec2(
                    nodeDims.x / 2,
                    nodeDims.y / 2
                )
            }

            val newPosX = mousePos.x - pinToMouseOffset.x
            val newPosY = mousePos.y - pinToMouseOffset.y

            ImNodes.setNodeEditorSpacePos(id, newPosX, newPosY)

            if(ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                pinToMouse = false
            }
        }

        lastPinToMouse = pinToMouse
    }

    protected fun noValue(attrib: Attribute): Nothing {
        raise(tr("err_attrib_nothandledby_this", attrib))
    }

    open fun drawNode() { }

    data class AnnotationData(val name: String,
                              val description: String,
                              val category: Category,
                              val showInList: Boolean)

    private class AttributeLabelListener

}