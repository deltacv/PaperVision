package io.github.deltacv.papervision.engine.bridge

import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.LocalPaperVisionEngine
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

class LocalPaperVisionEngineBridge(
    val paperVisionEngine: LocalPaperVisionEngine
) : PaperVisionEngineBridge {

    private val clients = mutableListOf<PaperVisionEngineClient>()

    init {
        paperVisionEngine.bridge = this
    }

    @Synchronized
    override fun connectClient(client: PaperVisionEngineClient) {
        clients.add(client)
    }

    @Synchronized
    override fun terminate(client: PaperVisionEngineClient) {
        clients.remove(client)
    }

    @Synchronized
    override fun sendMessage(client: PaperVisionEngineClient, message: PaperVisionEngineMessage) {
        if(!clients.contains(client)) {
            throw IllegalArgumentException("Client is not connected to this bridge")
        }

        paperVisionEngine.acceptMessage(message)
    }

    @Synchronized
    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        clients.forEach { client ->
            client.acceptResponse(response)
        }
    }

    @Synchronized
    override fun broadcastBytes(bytes: ByteArray) {
        clients.forEach { client ->
            client.acceptBytes(bytes)
        }
    }

}