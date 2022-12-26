package io.github.deltacv.easyvision.util.eocvsim

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.language.interpreted.JythonLanguage
import io.github.deltacv.easyvision.util.IpcClientWatchDog
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage
import io.github.deltacv.eocvsim.pipeline.PipelineSource

class EOCVSimIpcManager(
    val easyVision: EasyVision
) {

    val logger by loggerForThis()

    val ipcClient = IpcClientWatchDog()

    var previzSessionName: String? = null
        private set

    var previzState = EOCVSimPrevizState.NOT_RUNNING
        private set(value) {
            logger.info("EOCVSim previz state is currently $value")
            field = value
        }

    fun init() {
        ipcClient.onDisconnect {
            previzState = if(previzState == EOCVSimPrevizState.RUNNING) {
                EOCVSimPrevizState.RUNNING_BUT_NOT_CONNECTED
            } else {
                EOCVSimPrevizState.NOT_CONNECTED
            }
        }

        ipcClient.onConnect {
            if (previzState == EOCVSimPrevizState.RUNNING_BUT_NOT_CONNECTED) {
                previzState = EOCVSimPrevizState.RUNNING
            }
        }

        ipcClient.start()
    }

    fun stop() = ipcClient.stop()

    fun broadcast(message: IpcMessage) = ipcClient.broadcast(message)
    fun broadcastIfPossible(message: IpcMessage) = ipcClient.broadcastIfPossible(message)

    fun startPrevizSession(name: String) {
        previzSessionName = name

        previzState = if(ipcClient.isConnected) {
            EOCVSimPrevizState.RUNNING
        } else EOCVSimPrevizState.RUNNING_BUT_NOT_CONNECTED

        ipcClient.encourage()

        rebuildPreviz()
    }

    fun rebuildPreviz() {
        if(!previzState.running) return

        val code = easyVision.codeGenManager.build(previzSessionName!!, JythonLanguage, true)
        val name = previzSessionName!!

        if (ipcClient.isConnected) {
            previzState = EOCVSimPrevizState.RUNNING

            broadcast(
                PythonPipelineSourceMessage(name, code).onResponseWith<IpcBooleanResponse> {
                    if(!it.value) { // is not currently running
                        broadcast(
                            ChangePipelineMessage(name, PipelineSource.PYTHON_RUNTIME, true)
                        )
                    }
                }
            )
        } else {
            previzState = EOCVSimPrevizState.RUNNING_BUT_NOT_CONNECTED
        }
    }

    fun stopPrevizSession() {
        previzState = EOCVSimPrevizState.NOT_RUNNING
        previzSessionName = null
    }

}