package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.interpreted.JavascriptLanguage
import io.github.deltacv.easyvision.codegen.language.interpreted.PythonLanguage
import io.github.deltacv.easyvision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.easyvision.util.ElapsedTime
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PipelineSource
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage

class CodeGenManager(val easyVision: EasyVision) {

    fun build() {
        val timer = ElapsedTime()

        val codeGen = CodeGen("TestPipeline", PythonLanguage)
        easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)

        val code = codeGen.gen()

        println(code)

        EasyVision.eocvSimIpcClient.broadcast(
            PythonPipelineSourceMessage("ipc py test", code).onResponse {
                EasyVision.eocvSimIpcClient.broadcast(ChangePipelineMessage("ipc py test", PipelineSource.PYTHON_RUNTIME, true))
            }
        )
    }

}