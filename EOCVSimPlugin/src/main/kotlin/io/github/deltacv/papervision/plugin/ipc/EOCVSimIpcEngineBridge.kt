/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.plugin.ipc

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.plugin.ipc.serialization.ipcGson
import org.java_websocket.client.WebSocketClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class EOCVSimIpcEngineBridge(private val port: Int) : PaperVisionEngineBridge {

    private val clients = mutableListOf<PaperVisionEngineClient>()

    private var wsClient = WsClient(port, this)

    override val isConnected: Boolean
        get() = wsClient.isOpen

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

        private val logger = LoggerFactory.getLogger(this::class.java)

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