package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.action.editor.DeleteLinksAction
import io.github.deltacv.papervision.action.editor.DeleteNodesAction
import io.github.deltacv.papervision.gui.Popup
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.id.DrawableIdElement
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.util.flags

class RightClickMenuPopup(
    val nodeList: NodeList,
    val undo: () -> Unit,
    val redo: () -> Unit,
    val cut: (List<Node<*>>) -> Unit,
    val copy: (List<Node<*>>) -> Unit,
    val paste: () -> Unit,
    val selection: List<DrawableIdElement>
) : Popup() {

    override val title = "right click menu"
    override val flags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.Popup
    )

    override fun drawContents() {
        ImGui.pushStyleColor(ImGuiCol.Button, 0)

        if(ImGui.button(tr("mis_cut"))) {
            cut(selection.filter { it is Node<*> }.map { it as Node<*> })
            ImGui.closeCurrentPopup()
        }

        if(ImGui.button(tr("mis_copy"))) {
            copy(selection.filter { it is Node<*> }.map { it as Node<*> })
            ImGui.closeCurrentPopup()
        }

        if(ImGui.button(tr("mis_paste"))) {
            paste()
            ImGui.closeCurrentPopup()
        }

        if (selection.isNotEmpty()) {
            if (ImGui.button(tr("mis_delete"))) {
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

        if (ImGui.button(tr("mis_undo"))) {
            undo()
            ImGui.closeCurrentPopup()
        }

        if (ImGui.button(tr("mis_redo"))) {
            redo()
            ImGui.closeCurrentPopup()
        }

        ImGui.separator()

        if (ImGui.button(tr("mis_addnode"))) {
            nodeList.showList()
        }

        ImGui.popStyleColor()
    }
}