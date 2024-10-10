package io.github.deltacv.papervision.plugin.gui.imgui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.util.flags

class CloseConfirmWindow(
    val callback: (Action) -> Unit
) : Window() {
    enum class Action {
        YES,
        NO,
        CANCEL
    }

    override var title = "Confirm"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    override val isModal = true

    override fun onEnable() {
        focus = true
    }

    override fun drawContents() {
        ImGui.text("Do you wanna save before exiting?")
        ImGui.separator()

        if(ImGui.button("Yes")) {
            callback(Action.YES)
            ImGui.closeCurrentPopup()
        }
        ImGui.sameLine()
        if(ImGui.button("No")) {
            callback(Action.NO)
            ImGui.closeCurrentPopup()
        }
        ImGui.sameLine()
        if(ImGui.button("Cancel")) {
            callback(Action.CANCEL)
            ImGui.closeCurrentPopup()
        }
    }
}