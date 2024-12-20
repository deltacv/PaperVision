package io.github.deltacv.papervision.engine.client

typealias Handler = (Int, String, ByteArray) -> Unit

abstract class ByteMessageReceiver {

    private val handlers = mutableListOf<Handler>()

    fun callHandlers(id: Int, tag: String, bytes: ByteArray) {
        handlers.forEach { handler ->
            handler(id, tag, bytes)
        }
    }

    open fun addHandler(tag: String, handler: Handler) {
        handlers.add(handler)
    }

    open fun removeHandler(handlerInstance: Handler) {
        handlers.remove(handlerInstance)
    }

    open fun stop() { }

}