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

package io.github.deltacv.papervision.codegen

import imgui.ImGui
import imgui.ImVec2
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.Resolvable
import io.github.deltacv.papervision.exception.AttributeGenException
import io.github.deltacv.papervision.exception.GenException
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.ToastWindow
import io.github.deltacv.papervision.gui.DialogMessageWindow
import io.github.deltacv.papervision.gui.Popup
import io.github.deltacv.papervision.gui.TooltipPopup
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.id.container.IdContainer
import io.github.deltacv.papervision.id.container.IdContainerStack
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.util.hashCodeString
import io.github.deltacv.papervision.util.loggerForThis

class CodeGenManager(val paperVision: PaperVision) {

    val logger by loggerForThis()

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String? {
        val placeholders = IdContainer<Resolvable.Placeholder<*>>()

        IdContainerStack.local.push(placeholders) // all placeholders created during code gen will be caught here

        for(popup in IdContainerStack.local.peekNonNull<Popup>().inmutable) {
            if(popup.label == "Gen-Error") {
                popup.delete()
            }
        }
        paperVision.clearToasts()

        val codeGen = CodeGen(name, language, isForPreviz)

        logger.info("-- Starting CodeGen #${codeGen.hashCodeString} --")

        try {
            codeGen.stage = CodeGen.Stage.INITIAL_GEN

            paperVision.nodeEditor.outputNode.input.requireAttachedAttribute() // output always needs to be connected

            // start off code generation chain
            paperVision.nodeEditor.outputNode.genCodeIfNecessary(codeGen.current)

            codeGen.stage = CodeGen.Stage.END_GEN
        } catch (attrEx: AttributeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            TooltipPopup(
                attrEx.message,
                8.0,
                label = "Gen-Error"
            ) { ImVec2(attrEx.attribute.position.x + 5, attrEx.attribute.position.y + 20) }.enable()

            val node = attrEx.attribute.parentNode
            showError(codeGen, node, attrEx.message)

            logger.warn("-- CodeGen ${codeGen.hashCodeString} FAILED due to attribute exception --", attrEx)
            return null
        } catch(nodeEx: NodeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            TooltipPopup(
                nodeEx.message,
                8.0,
                label = "Gen-Error"
            ) { ImVec2(nodeEx.node.screenPosition.x, nodeEx.node.screenPosition.y - 20) }.enable()

            showError(codeGen, nodeEx.node, nodeEx.message)

            logger.warn("-- CodeGen ${codeGen.hashCodeString} FAILED due to node exception --", nodeEx)
            return null
        } catch(ex: Exception) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            DialogMessageWindow(
                message = "mis_codegen_error",
                title = "win_codegen_error",
                textArea = ex.stackTraceToString(),
                font = Font.find("calcutta-big")
            ).enable()

            logger.error("-- CodeGen #${codeGen.hashCodeString}FAILED due to ${if(ex is GenException) "gen" else "unknown"} exception --", ex)
            return null
        }

        val result = codeGen.build()

        codeGen.stage = CodeGen.Stage.ENDED_SUCCESS

        logger.debug("Flags defined during CodeGen #{}:{}", codeGen.hashCodeString, if(codeGen.flags().isEmpty()) " none" else "")
        for(flag in codeGen.flags()) {
            logger.debug("- $flag")
        }

        logger.info("-- CodeGen #${codeGen.hashCodeString} OK --")

        IdContainerStack.local.pop<Resolvable.Placeholder<*>>() // we're done with placeholders

        return result.trim()
    }

    private fun showError(codeGen: CodeGen, node: Node<*>, message: String) {
        if(!codeGen.isForPreviz) { // dont scroll if we're on an active previz session, can become annoying
            paperVision.nodeEditor.editorPanning.x = (-node.gridPosition.x) - (node.size.x / 2) + ImGui.getMainViewport().size.x / 2
            paperVision.nodeEditor.editorPanning.y = (-node.gridPosition.y) - (node.size.y / 2) + ImGui.getMainViewport().size.y / 2
        }

        val toast = if(node is DrawNode<*>) {
            tr("mis_codegen_erroron_toast", tr(node.annotationData.name))
        } else {
            tr("mis_codegen_errortoast")
        }

        ToastWindow(toast, 5.0, font = Font.find("calcutta-big")).enable()
    }

}
