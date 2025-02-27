/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.texteditor.TextEditor
import imgui.extension.texteditor.TextEditorLanguageDefinition
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.gui.util.TooltipPopup
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.platform.PlatformFileChooserResult
import io.github.deltacv.papervision.platform.PlatformFileFilter
import io.github.deltacv.papervision.platform.PlatformWindow
import io.github.deltacv.papervision.util.flags
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CodeDisplayWindow(
    val code: String,
    val name: String,
    val codeGenLanguage: Language,
    val editorLanguage: TextEditorLanguageDefinition,
    val platformWindow: PlatformWindow,
    val codeFont: Font? = null,
    val buttonsFont: Font? = null
) : Window() {
    override var title = "Code"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoScrollbar,
        ImGuiWindowFlags.NoScrollWithMouse
    )

    override val isModal = true

    val EDITOR = TextEditor()

    override fun onEnable() {
        focus = true

        EDITOR.isReadOnly

        EDITOR.languageDefinition = editorLanguage
        EDITOR.textLines = code.lines().toTypedArray()

        size = ImVec2(500f, 400f)
    }

    override fun drawContents() {
        codeFont?.let {
            ImGui.pushFont(it.imfont)
        }

        ImGui.beginChild(
            "${titleId}Child###$id", ImVec2(size.x, size.y * 0.85f),
            false, flags(ImGuiWindowFlags.HorizontalScrollbar)
        )

        EDITOR.render("${titleId}Code###$id")

        ImGui.endChild()

        codeFont?.let {
            ImGui.popFont()
        }

        buttonsFont?.let {
            ImGui.pushFont(it.imfont)
        }

        if(ImGui.button("Copy Code")) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                StringSelection(EDITOR.text), null
            )

            ToastWindow(
                tr("mis_codecopied"),
                3.0
            ).enable()
        }

        ImGui.sameLine()

        if(ImGui.button("Export to File")) {
            platformWindow.saveFileDialog(
                code.toByteArray(),
                "${name}.${codeGenLanguage.sourceFileExtension}",
                PlatformFileFilter("Source File", listOf(codeGenLanguage.sourceFileExtension))
            )
        }

        buttonsFont?.let {
            ImGui.popFont()
        }
    }
}