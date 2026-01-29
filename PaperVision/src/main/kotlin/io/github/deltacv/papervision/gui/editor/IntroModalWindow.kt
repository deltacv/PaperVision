package io.github.deltacv.papervision.gui.editor

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.alignForWidth
import io.github.deltacv.papervision.gui.centeredText
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.io.resourceToString
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags

class IntroModalWindow(
    val nodeEditor: NodeEditor,
    chooseLanguage: Boolean = nodeEditor.paperVision.setup.config.fields.shouldAskForLang
) : Window() {
    override var title = "win_welcome"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.HorizontalScrollbar
    )

    private val imguiFont = Font.find("default-12")
    private val monoFont = Font.find("jetbrains-mono")

    override val isModal = true

    val onDontShowAgain = PaperVisionEventHandler("IntroModalWindow-OnDontShowAgain")

    private var isFirstDraw = true

    private var choosingLanguage = chooseLanguage

    companion object {
        val icon = resourceToString("/ico/ico_ezv.txt")
        val iconLogo = resourceToString("/ico/ico_ezv_logo.txt")
    }

    override fun drawContents() {
        if(choosingLanguage) {
            ImGui.pushFont(monoFont.imfont)

            ImGui.newLine()

            val lines = tr("mis_welcomelanguage").split("\n")
            for(line in lines) {
                centeredText(line)
            }

            ImGui.newLine()

            var width = 0f
            width += ImGui.calcTextSize(tr("lan_en")).x
            width += ImGui.getStyle().itemSpacing.x + 30f
            width += ImGui.calcTextSize(tr("lan_es")).x

            val alignment = alignForWidth(width, 0.5f)

            if(ImGui.button(tr("lan_en"))) {
                nodeEditor.paperVision.changeLanguage("en")
                choosingLanguage = false
            }

            ImGui.sameLine(alignment + 30f + ImGui.calcTextSize(tr("lan_en")).x)

            if(ImGui.button(tr("lan_es"))) {
                nodeEditor.paperVision.changeLanguage("es")
                choosingLanguage = false
            }

            nodeEditor.paperVision.config.fields.lang = nodeEditor.paperVision.currentLanguage.lang
            nodeEditor.paperVision.config.fields.shouldAskForLang = false

            ImGui.popFont()

            centerWindow()
        } else {
            ImGui.newLine()
            ImGui.newLine()

            ImGui.pushFont(imguiFont.imfont)
            centeredText(icon)
            ImGui.popFont()

            ImGui.newLine()
            ImGui.newLine()

            ImGui.pushFont(monoFont.imfont)

            centeredText("mis_welcome1")
            centeredText("mis_welcome2")
            centeredText("mis_welcome3")
            centeredText("mis_welcome4")

            ImGui.newLine()

            centeredText("mis_welcome5")

            ImGui.newLine()

            var width = 0f
            width += ImGui.calcTextSize(tr("mis_gotit")).x
            width += ImGui.getStyle().itemSpacing.x
            width += ImGui.calcTextSize(tr("mis_dontshow_again")).x
            width += ImGui.getStyle().itemSpacing.x
            width += ImGui.calcTextSize(tr("mis_guidedtour")).x

            alignForWidth(width, 0.5f)

            if (ImGui.button(tr("mis_gotit"))) {
                delete()
            }

            ImGui.sameLine()

            if (ImGui.button(tr("mis_dontshow_again"))) {
                onDontShowAgain.run()
                delete()
            }

            ImGui.sameLine()

            if (ImGui.button(tr("mis_guidedtour"))) {
                GuidedTourWindow(nodeEditor).enable()
                delete()
            }

            ImGui.popFont()

            ImGui.sameLine()

            centerWindow()

            if (!isFirstDraw) {
                size = ImVec2(ImGui.getMainViewport().sizeX * 0.6f, ImGui.getWindowSizeY())
            }

            isFirstDraw = false
        }
    }
}
