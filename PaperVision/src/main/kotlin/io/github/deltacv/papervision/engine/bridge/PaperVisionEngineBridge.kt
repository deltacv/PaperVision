package io.github.deltacv.papervision.engine.bridge

import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

interface PaperVisionEngineBridge {
    val isConnected: Boolean

    fun connectClient(client: PaperVisionEngineClient)
    fun terminate(client: PaperVisionEngineClient)

    fun broadcastBytes(bytes: ByteArray)

    fun sendMessage(client: PaperVisionEngineClient, message: PaperVisionEngineMessage)
    fun acceptResponse(response: PaperVisionEngineMessageResponse)
}