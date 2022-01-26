package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.interpreted.JavascriptLanguage
import io.github.deltacv.easyvision.codegen.language.interpreted.PythonLanguage
import io.github.deltacv.easyvision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage
import io.github.deltacv.eocvsim.pipeline.PipelineSource

class CodeGenManager(val easyVision: EasyVision) {

    var isOnPrevizSession = false
        private set
    var previzSessionName: String? = null
        private set

    fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String {
        val codeGen = CodeGen(name, if(isForPreviz) PythonLanguage else language, isForPreviz)
        easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)

        val code = codeGen.gen()

        println(code)

        if(isForPreviz) {
            EasyVision.eocvSimIpcClient.broadcast(
                PythonPipelineSourceMessage(name, code).onResponse {
                    if(it is IpcBooleanResponse && !it.value) { // is not currently running
                        EasyVision.eocvSimIpcClient.broadcast(
                            ChangePipelineMessage(name, PipelineSource.PYTHON_RUNTIME,true)
                        )
                    }
                }
            )

            previzSessionName = name
            isOnPrevizSession = true
        }

        return code
    }

    fun startPreviz(name: String) = build(name, isForPreviz = true)

    fun rebuildPreviz() {
        if(isOnPrevizSession) {
            build(previzSessionName!!, isForPreviz = true)
        }
    }

    fun stopPreviz() {
        isOnPrevizSession = false
        previzSessionName = null
    }
}