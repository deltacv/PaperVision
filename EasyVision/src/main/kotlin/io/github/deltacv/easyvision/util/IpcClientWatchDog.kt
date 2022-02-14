package io.github.deltacv.easyvision.util

import io.github.deltacv.easyvision.util.event.EventHandler
import io.github.deltacv.eocvsim.ipc.IpcClient
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.security.PassToken
import org.slf4j.Logger
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

enum class PreviewState { NOT_CONNECTED, NOT_RUNNING, RUNNING }

class IpcClientWatchDog(
    val port: Int = 11026,
    val passToken: PassToken? = null,
    val timeoutSecs: Int = 3,
    val attemptsBeforeGivingUp: Int = 3
) {

    val logger by loggerForThis()

    private val runner by lazy { Runner(port, passToken, timeoutSecs, attemptsBeforeGivingUp, logger) }
    private val watchdogThread by lazy { Thread(runner, "IpcClientWatchDog-Thread") }

    val ipcClient get() = runner.ipcClient

    val isConnected get() = ipcClient.isOpen

    val onConnect get() = runner.onConnect
    val onDisconnect get() = runner.onDisconnect

    fun start() {
        if(!watchdogThread.isAlive) {
            watchdogThread.start()
        }
    }

    fun stop() {
        watchdogThread.interrupt()
    }

    fun binaryHandler(opcode: Byte, callback: (Short, ByteBuffer) -> Unit) =
        ipcClient.binaryHandler(opcode, callback)

    fun broadcast(message: IpcMessage) {
        if(ipcClient.isOpen) {
            ipcClient.broadcast(message)
        } else {
            synchronized(runner.queue) {
                runner.queue.add(message)
            }
            runner.encourage()
        }
    }

    fun broadcastIfPossible(message: IpcMessage) {
        if(ipcClient.isOpen) {
            ipcClient.broadcast(message)
        }
    }

    private class Runner(
        val port: Int,
        passToken: PassToken?,
        val timeoutSecs: Int,
        val attemptsBeforeGivingUp: Int,
        val logger: Logger
    ) : Runnable {

        val queue = ArrayList<IpcMessage>()

        val ipcClient = IpcClient(port, passToken)

        val onConnect = EventHandler("IpcClient-WatchDog-OnConnect")
        val onDisconnect = EventHandler("IpcClient-WatchDog-OnDisconnect")

        private var accumulatedTimeout = 0L
        private var currentAttempts = 0

        override fun run() {
            logger.info("Watchdog thread started")

            val timeout = (timeoutSecs * 1000).toLong()

            ipcClient.connectBlocking(timeout, TimeUnit.MILLISECONDS)
            if(ipcClient.isOpen) {
                onConnect.run()
            }

            while(!Thread.currentThread().isInterrupted) {
                try {
                    if((!ipcClient.isOpen || ipcClient.isClosed) && currentAttempts < attemptsBeforeGivingUp) {
                        onDisconnect.run()

                        ipcClient.reconnectBlocking()

                        if(ipcClient.isOpen) {
                            synchronized(queue) {
                                for(message in queue) {
                                    ipcClient.broadcast(message)
                                }

                                queue.clear()
                            }

                            onConnect.run()

                            accumulatedTimeout = 0
                        } else {
                            accumulatedTimeout += (timeout * 0.5).roundToLong()
                            currentAttempts++

                            logger.warn("Connection failed (probably timed out) to $port")
                        }
                    }

                    Thread.sleep(timeout + accumulatedTimeout)
                } catch(ignored: InterruptedException) { break }
            }

            if(ipcClient.isOpen) {
                ipcClient.close()
            }

            logger.info("Watchdog thread stopped")
        }

        fun encourage() {
            currentAttempts -= (currentAttempts * 0.5).toInt()
            accumulatedTimeout = 0
        }

    }

}