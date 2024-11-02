/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.engine.client

import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis
import java.util.concurrent.ArrayBlockingQueue

class ClientByteMessageReceiver : ByteMessageReceiver()

class PaperVisionEngineClient(
    val bridge: PaperVisionEngineBridge
) {

    val logger by loggerForThis()

    val byteReceiver = ClientByteMessageReceiver()

    val onProcess = PaperVisionEventHandler("PaperVisionEngineClient-OnProcess")

    private val messagesAwaitingResponse = mutableMapOf<Int, PaperVisionEngineMessage>()

    val processedBinaryMessagesHashes = ArrayBlockingQueue<Int>(100)

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
        val message = messagesAwaitingResponse[response.id] ?: return

        message.acceptResponse(response)

        if(!message.persistent) {
            messagesAwaitingResponse.remove(response.id)
        }
    }

    @Suppress("SENSELESS_COMPARISON") // uh, i have gotten NPEs from bytes being null somehow
    fun acceptBytes(bytes: ByteArray) {
        if(bytes == null) return

        synchronized(bytesQueue) {
            bytesQueue.add(bytes)
        }
    }

    fun sendMessage(message: PaperVisionEngineMessage) {
        messagesAwaitingResponse[message.id] = message
        bridge.sendMessage(this, message)
    }

    fun process() {
        synchronized(bytesQueue) {
            val binaryMessages = bytesQueue.toTypedArray()

            @Suppress("SENSELESS_COMPARISON")
            binaryMessages.forEach {
                if(it == null) return@forEach

                val tag = ByteMessageTag(ByteMessages.tagFromBytes(it))
                val id = ByteMessages.idFromBytes(it)

                bytesQueue.remove(it)

                byteReceiver.callHandlers(id, tag.toString(), it)

                if(processedBinaryMessagesHashes.size >= processedBinaryMessagesHashes.remainingCapacity()) {
                    processedBinaryMessagesHashes.poll()
                }
                processedBinaryMessagesHashes.offer(it.hashCode())
            }
        }

        onProcess.run()
    }
}