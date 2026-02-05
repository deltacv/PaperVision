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

package io.github.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.texteditor.TextEditorLanguageDefinition
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.engine.client.message.AskProjectGenClassNameMessage
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.editor.CodeDisplayWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.util.flags
import io.github.deltacv.papervision.util.loggerForThis

class SourceCodeExportSelectLanguageWindow(
    val paperVision: PaperVision,
    val nodeEditorSizeSupplier: () -> ImVec2
) : Window() {

    companion object {
        const val SEPARATION_MULTIPLIER = 1.5f
    }

    override var title = "$[win_selectlanguage]"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse
    )

    val fontAwesomeBrandsBig = Font.find("font-awesome-brands-big")

    override val modal = ModalMode.Modal()

    val logger by loggerForThis()

    override fun drawContents() {
        ImGui.pushFont(fontAwesomeBrandsBig.imfont)
        ImGui.pushStyleColor(ImGuiCol.Button, 0)

        if (ImGui.button(FontAwesomeIcons.Brands.Java)) {
            openSourceCodeWindow(JavaLanguage)
            delete()
        }

        ImGui.sameLine()
        ImGui.indent(ImGui.getItemRectSizeX() * SEPARATION_MULTIPLIER)

        if (ImGui.button(FontAwesomeIcons.Brands.Python)) {
            openSourceCodeWindow(CPythonLanguage)
            delete()
        }

        ImGui.popFont()
        ImGui.popStyleColor()
    }

    private fun openSourceCodeWindow(language: Language) {
        fun openWindow(code: String?, name: String, language: Language) {
            if (code == null) {
                logger.warn("Code generation failed, cancelled opening source code window")
                return
            }

            CodeDisplayWindow(
                code, name, language,
                TextEditorLanguageDefinition.CPlusPlus(),
                paperVision.window
            ).apply {
                enable()
                size = ImVec2(nodeEditorSizeSupplier().x * 0.8f, nodeEditorSizeSupplier().y * 0.8f)
            }
        }

        if (paperVision.engineClient.bridge.isConnected) {
            paperVision.engineClient.sendMessage(AskProjectGenClassNameMessage().onResponseWith<StringResponse> { response ->
                paperVision.onUpdate.once {
                    openWindow(paperVision.codeGenManager.build(response.value, language), response.value, language)
                }
            })
        } else {
            paperVision.onUpdate.once {
                openWindow(paperVision.codeGenManager.build("Mack", language), "Mack", language)
            }
        }
    }
}
