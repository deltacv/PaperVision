package io.github.deltacv.papervision.engine.client

typealias Handler = (Int, String, ByteArray) -> Unit

abstract class ByteMessageReceiver {

    private val handlers = mutableListOf<Handler>()

    fun callHandlers(id: Int, tag: String, bytes: ByteArray) {
        handlers.forEach { handler ->
            handler(id, tag, bytes)
        }
    }

    fun addHandler(tag: String, handler: Handler) {
        handlers.add(handler)
    }

    fun removeHandler(handlerInstance: Handler) {
        handlers.remove(handlerInstance)
    }

}