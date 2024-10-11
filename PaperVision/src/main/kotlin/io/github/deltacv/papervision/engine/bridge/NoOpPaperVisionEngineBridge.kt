package io.github.deltacv.papervision.engine.bridge

import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

object NoOpPaperVisionEngineBridge : PaperVisionEngineBridge {
    override val isConnected: Boolean
        get() = false

    override fun connectClient(client: PaperVisionEngineClient) {
        // No-op
    }

    override fun terminate(client: PaperVisionEngineClient) {
        // No-op
    }

    override fun broadcastBytes(bytes: ByteArray) {
        // No-op
    }

    override fun sendMessage(client: PaperVisionEngineClient, message: PaperVisionEngineMessage) {
        // No-op
    }

    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        // No-op
    }
}