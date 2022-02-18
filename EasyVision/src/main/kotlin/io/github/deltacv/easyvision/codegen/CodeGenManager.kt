package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.interpreted.PythonLanguage
import io.github.deltacv.easyvision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.easyvision.exception.AttributeGenException
import io.github.deltacv.easyvision.gui.util.Popup
import io.github.deltacv.easyvision.util.eocvsim.EOCVSimPrevizState
import io.github.deltacv.easyvision.util.loggerForThis

class CodeGenManager(val easyVision: EasyVision) {

    val logger by loggerForThis()

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String {
        for(popup in Popup.popups.inmutable) {
            if(popup.label == "Gen-Error") {
                popup.delete()
            }
        }

        val codeGen = CodeGen(name, language, isForPreviz)

        try {
            easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)
        } catch (attrEx: AttributeGenException) {
            Popup(attrEx.message, attrEx.attribute.position, 8.0, label = "Gen-Error").enable()
            logger.warn("Code gen stopped due to attribute exception", attrEx)
            return ""
        }

        return codeGen.gen()
    }

}