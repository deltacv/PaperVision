package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.flags

class ToastWindow(
    val message: String,
    val durationSeconds: Double = 5.0,
    val font: Font? = null
) : Window() {
    override var title = "toast"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoScrollbar,
    )

    val timer = ElapsedTime()

    override fun onEnable() {
        super.onEnable()
        timer.reset()
    }

    override fun drawContents() {
        position = ImVec2(
            ImGui.getMainViewport().centerX - size.x / 2,
            ImGui.getMainViewport().sizeY - size.y - 20
        )

        if(font != null) {
            ImGui.pushFont(font.imfont)
        }

        ImGui.text(tr(message))

        if(font != null) {
            ImGui.popFont()
        }

        if(timer.seconds > durationSeconds) {
            delete()
        }
    }
}