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

import imgui.ImVec2
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.exception.AttributeGenException
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.util.Popup
import io.github.deltacv.papervision.gui.util.TooltipPopup
import io.github.deltacv.papervision.id.IdElementContainerStack
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
                ImVec2(attrEx.attribute.position.x + 5, attrEx.attribute.position.y + 20),
                8.0,
                label = "Gen-Error"
            ).open()

            logger.warn("Code gen stopped due to attribute exception", attrEx)
            return null
        } catch(nodeEx: NodeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            TooltipPopup(
                nodeEx.message,
                ImVec2(nodeEx.node.screenPosition.x, nodeEx.node.screenPosition.y - 20),
                8.0,
                label = "Gen-Error"
            ).open()

            logger.warn("Code gen stopped due to node exception", nodeEx)
            return null
        }

        val result = codeGen.gen()

        codeGen.stage = CodeGen.Stage.ENDED_SUCCESS

        return result.trim()
    }

}