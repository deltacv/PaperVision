package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.util.Font
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

        for(window in idContainer.inmutable) {
            if(window is ToastWindow && window != this) {
                window.delete()
            }
        }

        firstDraw = true
    }

    private var firstDraw = true

    override fun drawContents() {
        if(font != null) {
            ImGui.pushFont(font.imfont)
        }

        val currentSize = if(firstDraw) {
            ImVec2(
                ImGui.calcTextSize(tr(message)).x + 10,
                ImGui.calcTextSize(tr(message)).y + 10
            )
        } else size

        position = ImVec2(
            ImGui.getMainViewport().centerX - currentSize.x / 2,
            ImGui.getMainViewport().sizeY - currentSize.y - 20
        )

        ImGui.text(tr(message))

        if(font != null) {
            ImGui.popFont()
        }

        if(timer.seconds > durationSeconds) {
            delete()
        }
    }
}