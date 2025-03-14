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

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.plugin.ipc.serialization.ipcGson
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.vision.external.util.Timestamped
import org.java_websocket.client.WebSocketClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class EOCVSimIpcEngineBridge(private val port: Int) : PaperVisionEngineBridge {

    companion object {
        var logHighFrequencyMessages = true
    }

    private val highFrequencyMessages = mutableMapOf<String, MutableList<Long>>()

    private val clients = mutableListOf<PaperVisionEngineClient>()

    override val onClientProcess = PaperVisionEventHandler("LocalPaperVisionEngineBridge-OnClientProcess")
    override val processedBinaryMessagesHashes = ArrayBlockingQueue<Int>(100)

    private var wsClient = WsClient(port, this)

    val logger by loggerForThis()

    override val isConnected: Boolean
        get() = wsClient.isOpen

    @Synchronized
    override fun connectClient(client: PaperVisionEngineClient) {
        clients.add(client)

        ensureConnected()

        client.onProcess {
            if(!clients.contains(client)) {
                it.removeThis()
                return@onProcess
            }

            while(client.processedBinaryMessagesHashes.remainingCapacity() != 0) {
                val poolValue = client.processedBinaryMessagesHashes.poll() ?: break
                processedBinaryMessagesHashes.add(poolValue)
            }

            onClientProcess.run()
        }
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

        if(logHighFrequencyMessages) {
            val messages = highFrequencyMessages.getOrPut(message::class.java.name) { mutableListOf() }
            messages.add(System.currentTimeMillis())

            if(messages.size > 20) { // buffer size
                messages.removeAt(0)
            }

            // calculate avg time delta
            val avgDelta = messages.zipWithNext().map {
                it.second - it.first
            }.average()

            if(avgDelta < 100) {
                // get calling class
                val callingClass = Thread.currentThread().stackTrace[2]
                logger.warn("Sent too often: ${message::class.java.name} - avg $avgDelta. Last sent at ${callingClass.className} line ${callingClass.lineNumber}")
            }
        }
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
    ) : WebSocketClient(URI.create("ws://127.0.0.1:$port")) {

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