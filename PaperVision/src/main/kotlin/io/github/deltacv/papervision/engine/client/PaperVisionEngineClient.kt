/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.engine.client

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.loggerForThis
import java.util.concurrent.ConcurrentHashMap

class ClientByteMessageReceiver : ByteMessageReceiver()

class PaperVisionEngineClient(
    val bridge: PaperVisionEngineBridge
) {

    private data class AwaitingMessageData(
        val message: PaperVisionEngineMessage,
        val timestamp: Long
    )

    val logger by loggerForThis()

    val byteReceiver = ClientByteMessageReceiver()

    val onProcess = PaperEventHandler("PaperVisionEngineClient-OnProcess")

    private val messagesAwaitingResponse = ConcurrentHashMap<Int, AwaitingMessageData>()

    private val bytesQueue = mutableListOf<ByteArray>()

    fun connect() {
        logger.info("Connecting through bridge ${bridge.javaClass.simpleName}")
        bridge.connectClient(this)
    }

    fun disconnect() {
        logger.info("Disconnecting from bridge ${bridge.javaClass.simpleName}")
        bridge.terminate(this)
    }

    fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        val data = messagesAwaitingResponse[response.id] ?: return

        data.message.acceptResponse(response)

        if(!data.message.persistent) {
            messagesAwaitingResponse.remove(response.id)
        }
    }

    @Suppress("SENSELESS_COMPARISON") // uh, I have gotten NPEs from bytes being null somehow
    fun acceptBytes(bytes: ByteArray) {
        synchronized(bytesQueue) {
            bytesQueue.add(bytes)
        }
    }

    fun sendMessage(message: PaperVisionEngineMessage) {
        messagesAwaitingResponse[message.id] = AwaitingMessageData(message, System.currentTimeMillis())
        bridge.sendMessage(this, message)
    }

    fun process() {
        for(data in messagesAwaitingResponse.values) {
            val timeMillis = System.currentTimeMillis() - data.timestamp
            data.message.acceptElapsedTime(timeMillis)
        }

        synchronized(bytesQueue) {
            val binaryMessages = bytesQueue.toTypedArray()

            @Suppress("SENSELESS_COMPARISON")
            binaryMessages.forEach {
                if(it == null) return@forEach

                val tag = ByteMessageTag(ByteMessages.tagFromBytes(it))
                val id = ByteMessages.idFromBytes(it)
                bytesQueue.remove(it)

                byteReceiver.callHandlers(id, tag.toString(), it, ByteMessages.messageLengthFromBytes(it))
            }
        }

        onProcess.run()
    }
}
