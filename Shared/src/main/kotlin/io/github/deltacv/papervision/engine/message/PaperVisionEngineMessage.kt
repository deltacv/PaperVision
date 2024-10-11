package io.github.deltacv.papervision.engine.message

fun interface OnResponseCallback {
    fun onResponse(response: PaperVisionEngineMessageResponse)
}

interface PaperVisionEngineMessage {
    val id: Int

    fun acceptResponse(response: PaperVisionEngineMessageResponse)

    fun onResponse(callback: OnResponseCallback): PaperVisionEngineMessage
}