package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.exception.AttributeGenException
import io.github.deltacv.papervision.exception.NodeGenException
import io.github.deltacv.papervision.gui.util.Tooltip
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.util.loggerForThis

class CodeGenManager(val paperVision: PaperVision) {

    val logger by loggerForThis()

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String? {
        for(popup in IdElementContainerStack.threadStack.peekNonNull<Tooltip>().inmutable) {
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

            codeGen.stage = CodeGen.Stage.PRE_END

            for(node in codeGen.endingNodes) {
                node.genCodeIfNecessary(current)
            }
        } catch (attrEx: AttributeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            Tooltip(attrEx.message, attrEx.attribute.position, 8.0, label = "Gen-Error").enable()
            logger.warn("Code gen stopped due to attribute exception", attrEx)
            return null
        } catch(nodeEx: NodeGenException) {
            codeGen.stage = CodeGen.Stage.ENDED_ERROR

            Tooltip(nodeEx.message, nodeEx.node.position, 8.0, label = "Gen-Error").enable()
            logger.warn("Code gen stopped due to node exception", nodeEx)
            return null
        }

        val result = codeGen.gen()

        codeGen.stage = CodeGen.Stage.ENDED_SUCCESS

        return result.trim()
    }

}