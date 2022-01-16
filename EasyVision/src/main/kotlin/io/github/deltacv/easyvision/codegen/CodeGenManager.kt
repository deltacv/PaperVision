package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.interpreted.JavascriptLanguage
import io.github.deltacv.easyvision.codegen.language.interpreted.PythonLanguage
import io.github.deltacv.easyvision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PipelineSource
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage

class CodeGenManager(val easyVision: EasyVision) {

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String {
        val timer = ElapsedTime()

        val codeGen = CodeGen(name, if(isForPreviz) PythonLanguage else language, isForPreviz)
        easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)

        val code = codeGen.gen()

        println(code)

        if(isForPreviz) {
            EasyVision.eocvSimIpcClient.broadcast(
                PythonPipelineSourceMessage(name, code).onResponse {
                    EasyVision.eocvSimIpcClient.broadcast(
                        ChangePipelineMessage(
                            name,
                            PipelineSource.PYTHON_RUNTIME,
                            true
                        )
                    )
                }
            )
        }

        return code
    }

}