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

    override val isModal = true

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
                paperVision.onUpdate.doOnce {
                    openWindow(paperVision.codeGenManager.build(response.value, language), response.value, language)
                }
            })
        } else {
            paperVision.onUpdate.doOnce {
                openWindow(paperVision.codeGenManager.build("Mack", language), "Mack", language)
            }
        }
    }
}