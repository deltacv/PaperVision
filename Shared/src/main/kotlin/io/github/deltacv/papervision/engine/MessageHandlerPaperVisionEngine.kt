package io.github.deltacv.papervision.engine

import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import kotlin.reflect.KClass

abstract class MessageHandlerPaperVisionEngine : PaperVisionEngine {

    private val messageHandlers = mutableMapOf<Class<out PaperVisionEngineMessage>, MessageHandlerCtx<*>.() -> Unit>()

    override fun acceptMessage(message: PaperVisionEngineMessage) {
        try {
            // find the handler with the topmost class
            val handler =
                messageHandlers.entries.find { (type, _) -> type.isAssignableFrom(message.javaClass) }?.value
            handler?.invoke(MessageHandlerCtx(this, message))
        } catch(e: Exception) {
            sendResponse(ErrorResponse(e.message ?: "An error occurred", e).apply {
                id = message.id
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: PaperVisionEngineMessage> setMessageHandlerOf(type: KClass<T>, handler: MessageHandlerCtx<T>.() -> Unit) {
        messageHandlers[type.java] = handler as MessageHandlerCtx<*>.() -> Unit
    }

    inline fun <reified T : PaperVisionEngineMessage> setMessageHandlerOf(noinline handler: MessageHandlerCtx<T>.() -> Unit) {
        setMessageHandlerOf(T::class, handler)
    }

}


class MessageHandlerCtx<T: PaperVisionEngineMessage>(
    val engine: MessageHandlerPaperVisionEngine,
    val message: T
) {
    fun respond(response: PaperVisionEngineMessageResponse) {
        response.id = message.id
        engine.sendResponse(response)
    }
}