package io.github.deltacv.papervision.engine

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import kotlin.reflect.KClass


class LocalPaperVisionEngine : PaperVisionEngine {

    var bridge: PaperVisionEngineBridge? = null

    private val messageQueue = mutableListOf<PaperVisionEngineMessage>()
    private val messageHandlers = mutableMapOf<Class<out PaperVisionEngineMessage>, PaperVisionMessageHandlerCtx<*>.() -> Unit>()

    override fun acceptMessage(message: PaperVisionEngineMessage) {
        synchronized(messageQueue) {
            messageQueue.add(message)
        }
    }

    override fun sendResponse(response: PaperVisionEngineMessageResponse) {
        bridge?.acceptResponse(response)
    }

    override fun sendBytes(bytes: ByteArray) {
        bridge?.broadcastBytes(bytes)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: PaperVisionEngineMessage> setMessageHandlerOf(type: KClass<T>, handler: PaperVisionMessageHandlerCtx<T>.() -> Unit) {
        messageHandlers[type.java] = handler as PaperVisionMessageHandlerCtx<*>.() -> Unit
    }

    inline fun <reified T : PaperVisionEngineMessage> setMessageHandlerOf(noinline handler: PaperVisionMessageHandlerCtx<T>.() -> Unit) {
        setMessageHandlerOf(T::class, handler)
    }

    fun process() {
        val messages = synchronized(messageQueue) {
            messageQueue.toTypedArray()
        }

        messages.forEach { message ->
            try {
                // find the handler with the topmost class
                val handler =
                    messageHandlers.entries.find { (type, _) -> type.isAssignableFrom(message.javaClass) }?.value
                handler?.invoke(PaperVisionMessageHandlerCtx(this, message))
            } catch(e: Exception) {
                sendResponse(ErrorResponse(e.message ?: "An error occurred", e).apply {
                    id = message.id
                })
            } finally {
                synchronized(messageQueue) {
                    messageQueue.remove(message)
                }
            }
        }
    }

}

class PaperVisionMessageHandlerCtx<T: PaperVisionEngineMessage>(
    val engine: LocalPaperVisionEngine,
    val message: T
) {
    fun respond(response: PaperVisionEngineMessageResponse) {
        response.id = message.id
        engine.sendResponse(response)
    }
}