package io.github.deltacv.papervision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import imgui.flag.ImGuiMouseButton
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.mai18n.tr
import java.lang.IllegalArgumentException
import java.util.concurrent.ArrayBlockingQueue

abstract class DrawNode<S: CodeGenSession>(
    allowDelete: Boolean = true,
    joinActionStack: Boolean = true
) : Node<S>(allowDelete, joinActionStack) {

    var nextNodePosition: ImVec2? = null

    var pinToMouse = false
    private var lastPinToMouse = false
    private var pinToMouseOffset = ImVec2()

    private var isFirstDraw = true

    private var changeQueue = ArrayBlockingQueue<Boolean>(50)

    val annotationData by lazy {
        val annotation = this.javaClass.getAnnotation(PaperNode::class.java)
            ?: throw IllegalArgumentException("Node ${javaClass.typeName} needs to have a @RegisterNode annotation")

        AnnotationData(annotation.name, annotation.description, annotation.category, annotation.showInList)
    }

    init {
        onChange {
            changeQueue.add(true)
        }
    }

    var titleColor = annotationData.category.color
    var titleHoverColor = annotationData.category.colorSelected

    override fun onEnable() {
        for(attribute in nodeAttributes) {
            attribute.onChange  { onChange.run() }
        }
    }

    open fun init() {}

    override fun draw() {
        val title = annotationData.name

        if(changeQueue.remainingCapacity() <= 1) {
            changeQueue.poll()
        }

        ImNodes.pushColorStyle(ImNodesCol.TitleBar, titleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, titleHoverColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, titleHoverColor)

        ImNodes.beginNode(id)
            ImNodes.beginNodeTitleBar()
                ImGui.textUnformatted(tr(title))
            ImNodes.endNodeTitleBar()

            drawNode()
            drawAttributes()
        ImNodes.endNode()

        if(isFirstDraw) {
            init()
            isFirstDraw = false
        }

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        nextNodePosition?.let {
            ImNodes.setNodeScreenSpacePos(id, it.x, it.y)
            nextNodePosition = null
        }

        if(pinToMouse) {
            val mousePos = ImGui.getMousePos()

            if(pinToMouse != lastPinToMouse) {
                val nodeDims = ImVec2()

                // i have no idea why this is needed
                ImNodes.getNodeDimensions(nodeDims, id)

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

    override fun pollChange() = changeQueue.poll() ?: false

    data class AnnotationData(val name: String,
                              val description: String,
                              val category: Category,
                              val showInList: Boolean)

}