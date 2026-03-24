package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.ImGuiEx
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr

class ConfirmationModalWindow(
    val message: String,
    title: String = "mis_confirmaction",
    val font: Font? = null
) : Window() {

    override var title = tr(title)

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    override val modal = ModalMode.Modal()

    val onConfirm = PaperEventHandler("ConfirmationModalWindow-$title-OnConfirm")
    val onCancel = PaperEventHandler("ConfirmationModalWindow-$title-OnCancel")

    override fun drawContents() {
        font?.let { ImGui.pushFont(it.imfont) }

        ImGui.text(tr(message))

        font?.let { ImGui.popFont() }

        ImGui.newLine()

        var width = 0f
        width += ImGui.calcTextSize(tr("mis_confirm")).x
        width += ImGui.getStyle().itemSpacing.x + 30f
        width += ImGui.calcTextSize(tr("mis_cancel")).x

        ImGuiEx.alignForWidth(width, 0.5f)

        if (ImGui.button(tr("mis_confirm"))) {
            onConfirm.run()
            delete()
        }

        ImGui.sameLine()

        if (ImGui.button(tr("mis_cancel"))) {
            onCancel.run()
            delete()
        }
    }
}