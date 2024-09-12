package io.github.deltacv.papervision.engine.message

abstract class PaperVisionEngineMessageBase : PaperVisionEngineMessage {

    companion object {
        private var idCount = -1

        @Synchronized fun nextId(): Int {
            idCount++
            return idCount
        }
    }

    @Transient
    private val onResponseCallbacks = mutableListOf<(PaperVisionEngineMessageResponse) -> Unit>()

    override val id = nextId()

    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        for(callback in onResponseCallbacks) {
            callback(response)
        }
    }

    @JvmName("onResponseTyped")
    inline fun <reified R: PaperVisionEngineMessageResponse> onResponse(crossinline callback: (R) -> Unit): PaperVisionEngineMessageBase {
        onResponse {
            if(it is R) {
                callback(it)
            }
        }

        return this
    }

    override fun onResponse(callback: (PaperVisionEngineMessageResponse) -> Unit): PaperVisionEngineMessageBase {
        onResponseCallbacks.add(callback)
        return this
    }

    inline fun <reified T : PaperVisionEngineMessageResponse> onResponseWith(crossinline callback: (T) -> Unit) =
        onResponse {
            if(it is T) {
                callback(it)
            }
        }

    override fun toString() = "MessageBase(type=\"${this::class.java.typeName}\", id=$id)"

}