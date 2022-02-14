package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.interpreted.PythonLanguage
import io.github.deltacv.easyvision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.easyvision.exception.AttributeGenException
import io.github.deltacv.easyvision.gui.util.Popup
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage
import io.github.deltacv.eocvsim.pipeline.PipelineSource

enum class EOCVSimPreviewState { NOT_CONNECTED, NOT_RUNNING, RUNNING, RUNNING_BUT_NOT_CONNECTED }

class CodeGenManager(val easyVision: EasyVision) {

    var previzSessionName: String? = null
        private set
    var eocvSimState = EOCVSimPreviewState.NOT_CONNECTED
        private set(value) {
            logger.info("EOCVSim state is currently $value")
            field = value
        }

    val logger by loggerForThis()

    init {
        easyVision.eocvSimIpcClient.onDisconnect {
            eocvSimState = if(eocvSimState == EOCVSimPreviewState.RUNNING) {
                EOCVSimPreviewState.RUNNING_BUT_NOT_CONNECTED
            } else {
                EOCVSimPreviewState.NOT_CONNECTED
            }
        }

        easyVision.eocvSimIpcClient.onConnect {
            if(eocvSimState == EOCVSimPreviewState.RUNNING_BUT_NOT_CONNECTED) {
                eocvSimState = EOCVSimPreviewState.RUNNING
            }
        }
    }

    fun build(name: String, language: Language = JavaLanguage) =
        build(name, language, false)

    private fun build(
        name: String,
        language: Language = JavaLanguage,
        isForPreviz: Boolean = false
    ): String {
        for(popup in Popup.popups.inmutable) {
            if(popup.label == "Gen-Error") {
                popup.delete()
                println("delete lol")
            }
        }

        val codeGen = CodeGen(name, if(isForPreviz) PythonLanguage else language, isForPreviz)

        try {
            easyVision.nodeEditor.inputNode.startGen(codeGen.currScopeProcessFrame)
        } catch (attrEx: AttributeGenException) {
            Popup(attrEx.message, attrEx.attribute.position, 8.0, label = "Gen-Error").enable()
            logger.warn("Code gen stopped due to attribute exception", attrEx)
            return ""
        }

        val code = codeGen.gen()

        if(isForPreviz) {
            previzSessionName = name

            if(easyVision.eocvSimIpcClient.isConnected) {
                eocvSimState = EOCVSimPreviewState.RUNNING

                EasyVision.eocvSimIpcClient.broadcast(
                    PythonPipelineSourceMessage(name, code).onResponse {
                        if(it is IpcBooleanResponse && !it.value) { // is not currently running
                            EasyVision.eocvSimIpcClient.broadcast(
                                ChangePipelineMessage(name, PipelineSource.PYTHON_RUNTIME,true)
                            )
                        }
                    }
                )
            } else {
                eocvSimState = EOCVSimPreviewState.RUNNING_BUT_NOT_CONNECTED
            }
        }

        return code
    }

    fun startPreviz(name: String) = build(name, isForPreviz = true)

    fun rebuildPreviz() {
        if(eocvSimState == EOCVSimPreviewState.RUNNING || eocvSimState == EOCVSimPreviewState.RUNNING_BUT_NOT_CONNECTED) {
            build(previzSessionName!!, isForPreviz = true)
        }
    }

    fun stopPreviz() {
        eocvSimState = EOCVSimPreviewState.NOT_RUNNING
        previzSessionName = null
    }
}