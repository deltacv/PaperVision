package io.github.deltacv.papervision.plugin.gui.imgui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.util.flags

class EasyVisionIntroWindow : Window() {
    override var title = "Welcome!"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    private var isFirstDraw = true

    override fun onEnable() {
        centerWindow()
        focus = true
        isFirstDraw = true
    }

    override fun drawContents() {
        // Recenter the window on the second draw
        if (!isFirstDraw) {
            centerWindow()
        } else {
            isFirstDraw = false
        }

        ImGui.text("Welcome to EasyVision!")
        ImGui.separator()
    }

}