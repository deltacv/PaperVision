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

package io.github.deltacv.papervision.codegen

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.exception.AttributeGenException
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.ToastWindow
import io.github.deltacv.papervision.gui.util.DialogMessageWindow
import io.github.deltacv.papervision.gui.util.Popup
import io.github.deltacv.papervision.gui.util.TooltipPopup
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.util.loggerForThis

class CodeGenManager(val paperVision: PaperVision) {

    val logger by loggerForThis()

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String? {
        for(popup in IdElementContainerStack.threadStack.peekNonNull<Popup>().inmutable) {
            if(popup.label == "Gen-Error") {
                popup.delete()
            }
        }
        paperVision.clearToasts()

        val codeGen = CodeGen(name, language, isForPreviz)

        val current = codeGen.currScopeProcessFrame

        try {
            codeGen.stage = CodeGen.Stage.INITIAL_GEN

            paperVision.nodeEditor.outputNode.input.requireAttachedAttribute()

            paperVision.nodeEditor.inputNode.startGen(current)

            codeGen.stage = CodeGen.Stage.END_GEN

            for(node in codeGen.endingNodes) {
                node.genCodeIfNecessary(current)
            }
        } catch (attrEx: AttributeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            TooltipPopup(
                attrEx.message,
                { ImVec2(attrEx.attribute.position.x + 5, attrEx.attribute.position.y + 20) },
                8.0,
                label = "Gen-Error"
            ).open()

            val node = attrEx.attribute.parentNode
            showError(codeGen, node, attrEx.message)

            logger.warn("Code gen stopped due to attribute exception", attrEx)
            return null
        } catch(nodeEx: NodeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            TooltipPopup(
                nodeEx.message,
                { ImVec2(nodeEx.node.screenPosition.x, nodeEx.node.screenPosition.y - 20) },
                8.0,
                label = "Gen-Error"
            ).open()

            showError(codeGen, nodeEx.node, nodeEx.message)

            logger.warn("Code gen stopped due to node exception", nodeEx)
            return null
        } catch(ex: Exception) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            DialogMessageWindow(
                tr("win_codegen_error"),
                tr("mis_codegen_error"),
                ex.stackTraceToString(),
                font = paperVision.defaultFontBig
            ).enable()

            logger.error("Code gen stopped due to unknown exception", ex)
            return null
        }

        val result = codeGen.gen()

        codeGen.stage = CodeGen.Stage.ENDED_SUCCESS

        return result.trim()
    }

    private fun showError(codeGen: CodeGen, node: Node<*>, message: String) {
        if(!codeGen.isForPreviz) {
            paperVision.nodeEditor.editorPanning.x = (-node.gridPosition.x) - (node.size.x / 2) + ImGui.getMainViewport().size.x / 2
            paperVision.nodeEditor.editorPanning.y = (-node.gridPosition.y) - (node.size.y / 2) + ImGui.getMainViewport().size.y / 2
        }

        val toast = if(node is DrawNode<*>) {
            tr("mis_codegen_erroron_toast", tr(node.annotationData.name))
        } else {
            tr("mis_codegen_errortoast")
        }

        ToastWindow(toast, 5.0, font = paperVision.defaultFontBig).enable()
    }

}