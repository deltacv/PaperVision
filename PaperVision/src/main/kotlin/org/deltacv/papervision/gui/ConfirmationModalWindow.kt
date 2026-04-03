package org.deltacv.papervision.gui

import imgui.flag.ImGuiWindowFlags
import org.deltacv.papervision.gui.compose.dsl.composeRender
import org.deltacv.papervision.gui.compose.property.type.Text
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.util.event.PaperEventHandler
import org.deltacv.papervision.util.flags
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
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoCollapse
    )

    override val modal = ModalMode.Modal()

    val onConfirm = PaperEventHandler("ConfirmationModalWindow-$title-OnConfirm")
    val onCancel = PaperEventHandler("ConfirmationModalWindow-$title-OnCancel")

    override fun drawContents() = composeRender {
        alignedText(Text(message, font), 0.5)

        newLine()

        alignedRow(0.5) {
            button("mis_confirm") {
                onConfirm.run()
                delete()
            }

            button("mis_cancel") {
                onCancel.run()
                delete()
            }
        }
    }
}



