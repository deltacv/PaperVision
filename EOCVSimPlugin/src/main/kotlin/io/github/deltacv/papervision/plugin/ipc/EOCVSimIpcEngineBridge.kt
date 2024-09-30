package io.github.deltacv.papervision.plugin.ipc

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.plugin.ipc.serialization.ipcGson
import org.java_websocket.client.WebSocketClient
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class EOCVSimIpcEngineBridge(private val port: Int) : PaperVisionEngineBridge {

    private val clients = mutableListOf<PaperVisionEngineClient>()

    private var wsClient = WsClient(port, this)

    @Synchronized
    override fun connectClient(client: PaperVisionEngineClient) {
        clients.add(client)

        ensureConnected()
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

        ensureConnected()

        val messageJson = ipcGson.toJson(message)

        wsClient.send(messageJson)
    }

    private fun ensureConnected() {
        if(!wsClient.isOpen) {
            wsClient = WsClient(port, this)
            wsClient.connectBlocking(5, TimeUnit.SECONDS)
        }
    }

    override fun broadcastBytes(bytes: ByteArray) {
        for(client in clients) {
            client.acceptBytes(bytes)
        }
    }

    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        for(client in clients) {
            client.acceptResponse(response)
        }
    }

    class WsClient(
        val port: Int,
        val bridge: EOCVSimIpcEngineBridge
    ) : WebSocketClient(URI.create("ws://localhost:$port")) {
        val logger by loggerForThis()

        override fun onOpen(handshakedata: org.java_websocket.handshake.ServerHandshake?) {
            logger.info("Connected to server in $port")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            logger.info("Disconnected from server")
        }

        override fun onMessage(message: String) {
            val response = ipcGson.fromJson(message, PaperVisionEngineMessageResponse::class.java)
            bridge.acceptResponse(response)
        }

        override fun onMessage(bytes: ByteBuffer) {
            bridge.broadcastBytes(bytes.array())
        }

        override fun onError(ex: Exception) {
            logger.error("Error on connection", ex)
        }
    }
}