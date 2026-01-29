package io.github.deltacv.papervision.engine.client

typealias Handler = (Int, String, ByteArray, Int) -> Unit

abstract class ByteMessageReceiver {

    private val handlers = mutableListOf<Handler>()

    fun callHandlers(id: Int, tag: String, bytes: ByteArray, messageLength: Int) {
        handlers.forEach { handler ->
            handler(id, tag, bytes, messageLength)
        }
    }

    open fun addHandler(handler: Handler) {
        handlers.add(handler)
    }

    open fun removeHandler(handlerInstance: Handler) {
        handlers.remove(handlerInstance)
    }

    open fun stop() { }

}
