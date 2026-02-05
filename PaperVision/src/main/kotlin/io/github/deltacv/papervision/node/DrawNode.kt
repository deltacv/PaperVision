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

package io.github.deltacv.papervision.node

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import imgui.flag.ImGuiMouseButton
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.mai18n.tr
import java.lang.IllegalArgumentException
import java.util.concurrent.ArrayBlockingQueue

abstract class DrawNode<S: CodeGenSession>(
    allowDelete: Boolean = true,
    joinActionStack: Boolean = true
) : Node<S>(allowDelete, joinActionStack) {

    var nextNodePosition: ImVec2? = null

    var pinToMouse = false
    var pinToMouseOffset = ImVec2()

    override val genNodeName: String?
        get() = "${tr(annotationData.name)} (#$id)"

    private var lastPinToMouse = false
    private var pinToMouseNewOffset = ImVec2()

    private var isFirstDraw = true

    private var changeQueue = ArrayBlockingQueue<Boolean>(50)

    val annotationData by lazy {
        val annotation = this.javaClass.getAnnotation(PaperNode::class.java)
            ?: throw IllegalArgumentException("Node ${javaClass.typeName} needs to have a @PaperNode annotation")

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

        nextNodePosition?.let {
            ImNodes.setNodeEditorSpacePos(id, it.x, it.y)
            nextNodePosition = null
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

        ImNodes.getNodeDimensions(size, id)
        ImNodes.getNodeEditorSpacePos(position, id)
        ImNodes.getNodeScreenSpacePos(screenPosition, id)
        ImNodes.getNodeGridSpacePos(gridPosition, id)

        if(isFirstDraw) {
            init()
            isFirstDraw = false
        }

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        if(pinToMouse) {
            val mousePos = ImGui.getMousePos()

            if(pinToMouse != lastPinToMouse) {
                val nodeDims = ImVec2()

                // i have no idea why this is needed
                ImNodes.getNodeDimensions(nodeDims, id)

                pinToMouseNewOffset = ImVec2(
                    (nodeDims.x / 2) + pinToMouseOffset.x,
                    (nodeDims.y / 2) + pinToMouseOffset.y
                )
            }

            val newPosX = mousePos.x - pinToMouseNewOffset.x
            val newPosY = mousePos.y - pinToMouseNewOffset.y

            ImNodes.setNodeEditorSpacePos(id, newPosX, newPosY)

            if(ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                pinToMouse = false
            }
        }

        lastPinToMouse = pinToMouse
    }

    protected fun noValue(attrib: Attribute): Nothing {
        raise(tr("err_attrib_nothandled_bythis", attrib))
    }

    open fun drawNode() { }

    override fun pollChange() = changeQueue.poll() ?: false

    data class AnnotationData(val name: String,
                              val description: String,
                              val category: Category,
                              val showInList: Boolean)

}
