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
import io.github.deltacv.papervision.engine.MessageHandlerPaperVisionEngine
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.plugin.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class EOCVSimIpcEngine : MessageHandlerPaperVisionEngine() {

    val logger by loggerForThis()

    val server = WsServer(this).apply {
        start()
    }

    override fun sendBytes(bytes: ByteArray){
        server.broadcast(bytes)
    }

    override fun sendResponse(response: PaperVisionEngineMessageResponse) {
        server.broadcast(ipcGson.toJson(response))
    }

    class WsServer(val engine: EOCVSimIpcEngine) : WebSocketServer(InetSocketAddress("127.0.0.1",0)) {
        val logger by loggerForThis()

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
            // only allow connections from localhost
            val hostString = conn.localSocketAddress.hostString
            if(hostString != "127.0.0.1" && hostString != "localhost" && hostString != "0.0.0.0") {
                logger.warn("Connection from ${conn.remoteSocketAddress} refused, only localhost connections are allowed")
                conn.close(1013, "Ipc does not allow connections incoming from non-localhost addresses")
            }

            logger.info("New connection: ${conn.remoteSocketAddress}")
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            logger.info("Connection closed: ${conn?.remoteSocketAddress}")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            val engineMessage = ipcGson.fromJson(message, PaperVisionEngineMessage::class.java)

            logger.trace("Message {}: {}", engineMessage::class.java.simpleName, message)
            engine.acceptMessage(engineMessage)
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            logger.error("Error on connection ${conn?.remoteSocketAddress}", ex)
        }

        override fun onStart() {
            logger.info("WebSocket server started on port ${address.port}")
        }
    }
}