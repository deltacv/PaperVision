package io.github.deltacv.papervision.gui.editor.button

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.gui.editor.menu.SourceCodeExportSelectLanguageWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags

class SourceCodeExportButtonWindow(
    val floatingButtonSupplier: () -> NodeList.FloatingButton,
    val nodeEditorSizeSupplier: () -> ImVec2,
    val paperVision: PaperVision
) : Window() {
    override var title = ""
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    val fontAwesomeBig get() = Font.find("font-awesome-big")

    val frameWidth get() = floatingButtonSupplier().frameWidth

    var isPressed = false
        private set

    override fun preDrawContents() {
        position = ImVec2(
            floatingButtonSupplier().position.x - NodeList.Companion.PLUS_FONT_SIZE * 1.7f,
            floatingButtonSupplier().position.y,
        )
    }

    override fun drawContents() {
        ImGui.pushFont(fontAwesomeBig.imfont)

        isPressed = ImGui.button(
            FontAwesomeIcons.FileCode,
            floatingButtonSupplier().frameWidth,
            floatingButtonSupplier().frameWidth
        )

        if (isPressed) {
            SourceCodeExportSelectLanguageWindow(paperVision, nodeEditorSizeSupplier).enable()
        }

        ImGui.popFont()
    }
}
