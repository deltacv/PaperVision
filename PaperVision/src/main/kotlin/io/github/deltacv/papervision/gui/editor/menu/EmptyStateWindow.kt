package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.compose.dsl.composeRender
import io.github.deltacv.papervision.gui.compose.property.type.Text
import io.github.deltacv.papervision.gui.compose.property.type.asProperty
import io.github.deltacv.papervision.gui.editor.NodeEditor
import io.github.deltacv.papervision.gui.style.opacity
import io.github.deltacv.papervision.gui.font.Font
import io.github.deltacv.papervision.util.flags

class EmptyStateWindow(val editor: NodeEditor) : Window() {
    override var title = "empty state"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoSavedSettings,
        ImGuiWindowFlags.NoFocusOnAppearing,
        ImGuiWindowFlags.NoNav
    )

    override fun preDrawContents() {
        ImGui.pushStyleColor(ImGuiCol.WindowBg, ImGui.getStyle().getColor(ImGuiCol.WindowBg).opacity(0.3f))
        super.preDrawContents()
    }

    override fun drawContents() {
        composeRender {
            val bigFont = Font.find("calcutta-big")

            alignedText(Text("mis_nodeeditor_emptystate1", bigFont), 0.5)
            alignedText(Text("mis_nodeeditor_emptystate2", bigFont), 0.5)

            newLine()

            alignedRow(0.5) {
                button("mis_guidedtour") {
                    GuidedTourWindow(editor).enable()
                    delete()
                }

                text("mis_lowercase_or")

                button("mis_addnode") {
                    editor.nodeList.showList()
                    delete()
                }
            }
        }

        centerWindow(editor.editorPanning)

        ImGui.popStyleColor()
    }
}