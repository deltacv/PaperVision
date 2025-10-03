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

package io.github.deltacv.papervision.engine.bridge

import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.LocalPaperVisionEngine
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import java.util.concurrent.ArrayBlockingQueue

class LocalPaperVisionEngineBridge(
    val paperVisionEngine: LocalPaperVisionEngine
) : PaperVisionEngineBridge {

    private val clients = mutableListOf<PaperVisionEngineClient>()

    override val onClientProcess = PaperVisionEventHandler("LocalPaperVisionEngineBridge-OnClientProcess")

    override val isConnected: Boolean
        get() = true

    init {
        paperVisionEngine.bridge = this
    }

    @Synchronized
    override fun connectClient(client: PaperVisionEngineClient) {
        clients.add(client)

        client.onProcess {
            if(!clients.contains(client)) {
                it.removeThis()
                return@onProcess
            }
        }
    }

    @Synchronized
    override fun terminate(client: PaperVisionEngineClient) {
        clients.remove(client)
    }

    @Synchronized
    override fun sendMessage(client: PaperVisionEngineClient, message: PaperVisionEngineMessage) {
        if(!clients.contains(client)) {
            throw IllegalArgumentException("Client is not connected to this bridge")
        }

        paperVisionEngine.acceptMessage(message)
    }

    @Synchronized
    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        clients.forEach { client ->
            client.acceptResponse(response)
        }
    }

    @Synchronized
    override fun broadcastBytes(bytes: ByteArray) {
        clients.forEach { client ->
            client.acceptBytes(bytes)
        }
    }

}