package io.github.deltacv.easyvision.io

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.StartStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StopStreamingMessage
import java.lang.IllegalStateException

class PipelineStream(
    val easyVision: EasyVision,
    val width: Int = 320,
    val height: Int = 240
) {

    companion object {
        const val opcode: Byte = 0xE
    }

    var isStarting = false
        private set
    var isStarted = false
        private set

    val ipcClient get() = easyVision.eocvSimIpcClient

    val queue = TextureProcessorQueue(easyVision)

    init {
        ipcClient.binaryHandler(opcode) { id, bytes ->
            if(isStarted) {
                queue.offer(id.toInt(), width, height, bytes)
            }
        }
    }

    fun startIfNeeded() {
        if(!isStarted && !isStarting) {
            isStarting = false

            ipcClient.broadcast(StartStreamingMessage(width, height, opcode).onResponse {
                if(it is IpcOkResponse) {
                    isStarted = true
                } else if(it is IpcErrorResponse && it.exception is IllegalStateException) {
                    isStarted = true
                }
                isStarting = false
            })
        }
    }

    fun stop() {
        if(isStarted) {
            ipcClient.broadcast(StopStreamingMessage().onResponse {
                if(it is IpcOkResponse) {
                    isStarted = false
                }
            })
        }
    }

    fun textureOf(id: Int) = queue[id]

    fun clear() = queue.clear()

}