package io.github.deltacv.papervision.engine

import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import java.nio.ByteBuffer

interface PaperVisionEngine {
    fun sendBytes(bytes: ByteArray)
    fun sendBytes(tag: ByteArray, id: Int, bytes: ByteArray) = sendBytes(ByteMessages.toBytes(tag, id, bytes))
    fun sendBytes(tag: ByteMessageTag, id: Int, bytes: ByteArray) = sendBytes(tag.tag, id, bytes)

    fun acceptMessage(message: PaperVisionEngineMessage)
    fun sendResponse(response: PaperVisionEngineMessageResponse)
}