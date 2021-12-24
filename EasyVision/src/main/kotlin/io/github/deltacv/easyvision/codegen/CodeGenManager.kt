package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.JavaLanguage
import io.github.deltacv.easyvision.codegen.language.KotlinLanguage
import io.github.deltacv.easyvision.util.ElapsedTime

class CodeGenManager(val easyVision: EasyVision) {

    fun build() {
        val timer = ElapsedTime()

        val codeGen = CodeGen("TestPipeline", KotlinLanguage)
        easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)

        println(codeGen.gen())
        println("took ${timer.seconds}")
    }

}