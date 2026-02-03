package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.Option
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.util.flags
import kotlin.collections.iterator

class OptionsWindow(
    val options: Map<String, Option>
) : Window() {
    override var title = "$[win_options]"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    val tooltipFont = Font.find("calcutta-big")
    val fontAwesomeBig = Font.find("font-awesome-big")

    override val modal = ModalMode.Modal()

    override fun drawContents() {
        ImGui.pushFont(fontAwesomeBig.imfont)
        ImGui.pushStyleColor(ImGuiCol.Button, 0)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0)

        for ((name, option) in options) {
            if (ImGui.button(name)) {
                option.action()
                delete()
            }

            if(ImGui.isItemHovered()) {
                ImGui.pushFont(tooltipFont.imfont)
                ImGui.setTooltip(tr(option.description))
                ImGui.popFont()
            }

            ImGui.sameLine()
            ImGui.indent(ImGui.getItemRectSizeX() * SourceCodeExportSelectLanguageWindow.Companion.SEPARATION_MULTIPLIER)
        }

        ImGui.popStyleColor()
        ImGui.popStyleColor()
        ImGui.popFont()
    }
}
