package io.github.deltacv.papervision.engine.message

interface PaperVisionEngineMessage {
    val id: Int

    fun acceptResponse(response: PaperVisionEngineMessageResponse)

    fun onResponse(callback: (PaperVisionEngineMessageResponse) -> Unit): PaperVisionEngineMessage
}