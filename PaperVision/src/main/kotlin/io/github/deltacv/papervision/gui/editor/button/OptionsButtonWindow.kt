package io.github.deltacv.papervision.gui.editor.button

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.gui.editor.Option
import io.github.deltacv.papervision.gui.editor.menu.OptionsWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags

class OptionsButtonWindow(
    val eocvSimPlayButtonWindow: PlayButtonWindow,
    val paperVision: PaperVision,
    val options: Map<String, Option>,
) : Window() {


    val tooltipFont = Font.find("calcutta-big")
    val fontAwesome = Font.find("font-awesome-big")

    override var title = "options control"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    private var lastButton = false

    var isPressed = false
        private set

    override fun preDrawContents() {
        val floatingButton = eocvSimPlayButtonWindow

        position = ImVec2(
            floatingButton.position.x - NodeList.Companion.PLUS_FONT_SIZE * 1.7f,
            floatingButton.position.y,
        )
    }

    override fun drawContents() {
        val floatingButton = eocvSimPlayButtonWindow

        ImGui.pushFont(fontAwesome.imfont)

        val text = FontAwesomeIcons.Gear

        isPressed = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

        if (lastButton != isPressed && isPressed) {
            OptionsWindow(options).enable()
        }

        ImGui.popFont()

        lastButton = isPressed
    }
}