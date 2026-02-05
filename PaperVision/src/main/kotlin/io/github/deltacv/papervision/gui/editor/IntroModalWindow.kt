/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui.editor

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.ImGuiEx
import io.github.deltacv.papervision.io.resourceToString
import io.github.deltacv.papervision.util.event.PaperEventHandler
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

    override val modal = ModalMode.Modal(closeOnOutsideClick = false)

    val onDontShowAgain = PaperEventHandler("IntroModalWindow-OnDontShowAgain")

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
                ImGuiEx.centeredText(line)
            }

            ImGui.newLine()

            var width = 0f
            width += ImGui.calcTextSize(tr("lan_en")).x
            width += ImGui.getStyle().itemSpacing.x + 30f
            width += ImGui.calcTextSize(tr("lan_es")).x

            val alignment = ImGuiEx.alignForWidth(width, 0.5f)

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
            ImGuiEx.centeredText(icon)
            ImGui.popFont()

            ImGui.newLine()
            ImGui.newLine()

            ImGui.pushFont(monoFont.imfont)

            ImGuiEx.centeredText("mis_welcome1")
            ImGuiEx.centeredText("mis_welcome2")
            ImGuiEx.centeredText("mis_welcome3")
            ImGuiEx.centeredText("mis_welcome4")

            ImGui.newLine()

            ImGuiEx.centeredText("mis_welcome5")

            ImGui.newLine()

            var width = 0f
            width += ImGui.calcTextSize(tr("mis_gotit")).x
            width += ImGui.getStyle().itemSpacing.x
            width += ImGui.calcTextSize(tr("mis_dontshow_again")).x
            width += ImGui.getStyle().itemSpacing.x
            width += ImGui.calcTextSize(tr("mis_guidedtour")).x

            ImGuiEx.alignForWidth(width, 0.5f)

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
