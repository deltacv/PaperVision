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

package io.github.deltacv.papervision.engine

import io.github.deltacv.papervision.engine.message.ByteMessageTag
import io.github.deltacv.papervision.engine.message.ByteMessages
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

interface PaperVisionEngine {
    fun sendBytes(bytes: ByteArray)
    fun sendBytes(tag: ByteArray, id: Int, bytes: ByteArray) = sendBytes(ByteMessages.toBytes(tag, id, bytes))
    fun sendBytes(tag: ByteMessageTag, id: Int, bytes: ByteArray) = sendBytes(tag.content, id, bytes)

    fun acceptMessage(message: PaperVisionEngineMessage)
    fun sendResponse(response: PaperVisionEngineMessageResponse)
}