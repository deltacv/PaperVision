package io.github.deltacv.papervision.engine.client

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis

class PaperVisionEngineClient(val bridge: PaperVisionEngineBridge) {
    val logger by loggerForThis()

    val onProcess = PaperVisionEventHandler("PaperVisionEngineClient-OnProcess")

    private val messagesAwaitingResponse = mutableMapOf<Int, PaperVisionEngineMessage>()

    private val bytesQueue = mutableListOf<ByteArray>()
    private val byteMessageHandlers = mutableMapOf<ByteMessageTag, (ByteArray) -> Unit>()

    fun connect() {
        logger.info("Connecting through bridge ${bridge.javaClass.simpleName}")
        bridge.connectClient(this)
    }

    fun disconnect() {
        logger.info("Disconnecting from bridge ${bridge.javaClass.simpleName}")
        bridge.terminate(this)
    }

    fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        messagesAwaitingResponse.remove(response.id)?.acceptResponse(response)
    }

    fun acceptBytes(bytes: ByteArray) {
        synchronized(bytesQueue) {
            bytesQueue.add(bytes)
        }
    }

    fun sendMessage(message: PaperVisionEngineMessage) {
        messagesAwaitingResponse[message.id] = message
        bridge.sendMessage(this, message)
    }

    fun setByteMessageHandlerOf(tag: ByteMessageTag, handler: (ByteArray) -> Unit) {
        byteMessageHandlers[tag] = handler
    }

    fun process() {
        val binaryMessages = synchronized(bytesQueue) {
            bytesQueue.toTypedArray()
        }

        binaryMessages.forEach {
            val tag = ByteMessageTag(ByteMessages.tagFromBytes(it))

            val handler = byteMessageHandlers[tag] ?: return@forEach

            handler(it)
            bytesQueue.remove(it)
        }

        onProcess.run()
    }
}