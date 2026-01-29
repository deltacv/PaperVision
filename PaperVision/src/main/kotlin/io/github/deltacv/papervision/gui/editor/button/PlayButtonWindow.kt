package io.github.deltacv.papervision.gui.editor.button

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.engine.bridge.NoOpPaperVisionEngineBridge
import io.github.deltacv.papervision.gui.ToastWindow
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags

class PlayButtonWindow(
    val sourceCodeExportButton: SourceCodeExportButtonWindow,
    val paperVision: PaperVision,
) : Window() {

    val fontAwesome = Font.find("font-awesome-big")
    val defaultFontBig = Font.find("calcutta-big")

    override var title = "eocv sim control"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    private var lastButton = false

    var isPressed = false
        private set

    val frameWidth get() = sourceCodeExportButton.frameWidth

    override fun preDrawContents() {
        val floatingButton = sourceCodeExportButton

        position = ImVec2(
            floatingButton.position.x - NodeList.Companion.PLUS_FONT_SIZE * 1.7f,
            floatingButton.position.y,
        )
    }

    override fun drawContents() {
        val floatingButton = sourceCodeExportButton

        ImGui.pushFont(fontAwesome.imfont)

        val text = if (paperVision.previzManager.previzRunning) {
            FontAwesomeIcons.Stop
        } else FontAwesomeIcons.Play

        isPressed = ImGui.button(text, floatingButton.frameWidth, floatingButton.frameWidth)

        if (lastButton != isPressed && isPressed) {
            if(paperVision.engineClient.bridge is NoOpPaperVisionEngineBridge) {
                ToastWindow("err_noop_engine", font = defaultFontBig).enable()
            } else {
                if (!paperVision.previzManager.previzRunning) {
                    paperVision.startPrevizWithEngine()
                } else {
                    paperVision.previzManager.stopPreviz()
                }
            }
        }

        ImGui.popFont()

        lastButton = isPressed
    }
}
