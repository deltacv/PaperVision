package io.github.deltacv.papervision.plugin.project.recovery

import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.plugin.logging.SLF4JIOReceiver
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.File
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class RecoveryDaemonProcessManager(
    val pluginJarFile: File
) {

    val executor = Executors.newFixedThreadPool(2)

    private val server = WsServer(this)

    val logger by loggerForThis()

    fun start() {
        server.start()
    }

    private fun submitProcess(port: Int) {
        executor.submit {
            logger.info("Starting project recovery daemon process")

            val exit = JavaProcess.execClasspath(
                RecoveryDaemonClientMain::class.java,
                SLF4JIOReceiver(logger),
                pluginJarFile.path,
                null,
                listOf(port.toString())
            )

            logger.warn("Project recovery daemon process exited with code $exit")
        }
    }

    fun sendRecoveryData(recoveryData: RecoveryData) {
        for(conn in server.connections) {
            conn.send(RecoveryData.serialize(recoveryData))
        }
    }

    private class WsServer(
        val manager: RecoveryDaemonProcessManager
    ) : WebSocketServer(InetSocketAddress(0)) {
        val logger by loggerForThis()

        override fun onOpen(ws: WebSocket, p1: ClientHandshake?) {
            logger.info("Client connected ${ws.localSocketAddress}")
        }

        override fun onClose(ws: WebSocket, p1: Int, p2: String?, p3: Boolean) {
            logger.info("Client disconnected ${ws.localSocketAddress}")
        }

        override fun onMessage(p0: WebSocket?, p1: String?) { }

        override fun onError(p0: WebSocket?, p1: Exception?) {
            logger.error("Error in recovery daemon server", p1)
        }

        override fun onStart() {
            logger.info("Recovery daemon server started in port $port")
            manager.submitProcess(port)
        }
    }

}