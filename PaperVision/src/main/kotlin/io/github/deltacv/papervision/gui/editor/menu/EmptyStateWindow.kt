package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.compose.dsl.compose
import io.github.deltacv.papervision.gui.style.opacity
import io.github.deltacv.papervision.gui.util.ImGuiEx
import io.github.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr

class EmptyStateWindow : Window() {
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
        ImGui.pushStyleColor(ImGuiCol.WindowBg, ImGui.getStyle().getColor(ImGuiCol.WindowBg).opacity(0.5f))

        super.preDrawContents()
    }

    override fun drawContents() {
        compose {
            alignedText("mis_nodeeditor_emptystate1", 0.5f)
            alignedText("mis_nodeeditor_emptystate2", 0.5f)

            newLine()

            alignedRow(0.5f) {
                button("mis_addnode") {
                    delete()
                }

                text("mis_lowercase_or")

                button("mis_guidedtour") {
                    delete()
                }
            }
        }

        centerWindow()

        ImGui.popStyleColor()
    }
}