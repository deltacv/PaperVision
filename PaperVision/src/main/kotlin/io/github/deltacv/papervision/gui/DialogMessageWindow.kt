package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.ImGuiEx
import io.github.deltacv.papervision.id.Misc
import io.github.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.getValue

class DialogMessageWindow(
    override var title: String,
    val message: String,
    val textArea: String? = null,
    val font: Font? = null,
    override val modal: ModalMode = ModalMode.Modal()
): Window() {
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoScrollbar,
        ImGuiWindowFlags.NoScrollWithMouse
    )

    val textAreaId by Misc.newMiscId()

    override fun drawContents() {
        font?.let {
            ImGui.pushFont(it.imfont)
        }

        val messageSize = ImGui.calcTextSize(tr(message))

        var maxWidth = messageSize.x
        var height = messageSize.y

        ImGuiEx.centeredText(message)

        ImGui.setCursorPos(ImVec2(ImGui.getCursorPosX(), ImGui.getCursorPosY() - 20))

        if(textArea != null) {
            val textSize = ImGui.calcTextSize(tr(textArea))
            ImGui.inputTextMultiline("##$textAreaId", ImString(textArea), ImGui.calcTextSize(tr(textArea)).x, textSize.y + 20)

            height += textSize.y.coerceAtMost(ImGui.getMainViewport().sizeY * 0.6f)
            maxWidth = maxOf(maxWidth, textSize.x)
        }

        ImGui.dummy(0f, 15f)
        ImGui.newLine()

        val gotItSize = ImGui.calcTextSize(tr("mis_gotit"))
        height += gotItSize.y * 2

        var buttonsWidth = gotItSize.x
        if(textArea != null) {
            buttonsWidth += ImGui.calcTextSize(tr("mis_copytext")).y
        }

        ImGuiEx.alignForWidth(buttonsWidth, 0.5f)

        if(ImGui.button(tr("mis_gotit"))) {
            delete()
        }

        if(textArea != null) {
            ImGui.sameLine()

            if(ImGui.button(tr("mis_copytext"))) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(
                    StringSelection(textArea), null
                )
            }
        }

        font?.let {
            ImGui.popFont()
        }

        size = ImVec2(maxWidth + 10, height + 80)
    }
}