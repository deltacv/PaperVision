package io.github.deltacv.papervision.plugin.ipc

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.engine.MessageHandlerPaperVisionEngine
import io.github.deltacv.papervision.engine.PaperVisionEngine
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.plugin.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class EOCVSimIpcEngine : MessageHandlerPaperVisionEngine() {

    val server = WsServer(this).apply {
        start()
    }

    override fun sendBytes(bytes: ByteArray){
        server.broadcast(bytes)
    }

    override fun sendResponse(response: PaperVisionEngineMessageResponse) {
        server.broadcast(ipcGson.toJson(response))
    }

    class WsServer(val engine: EOCVSimIpcEngine) : WebSocketServer(InetSocketAddress(0)) {
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

            logger.trace("Message from {}: {}", engineMessage::class.java.simpleName, message)
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