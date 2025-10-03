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

package io.github.deltacv.papervision.engine.message

import java.nio.ByteBuffer

open class ByteMessageTag(val tag: ByteArray) {
    companion object {
        fun fromString(tag: String) = ByteMessageTag(tag.toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteMessageTag

        return tag.contentEquals(other.tag)
    }

    override fun toString() = tag.toString(Charsets.UTF_8)

    override fun hashCode() = tag.contentHashCode()
}

/**
 * Structure of a byte message:
 * - 4 bytes: int n
 * - n bytes: string tag
 * - 4 bytes: int id
 * - rest: the message
 */
object ByteMessages {

    fun tagFromBytes(bytes: ByteArray): ByteArray {
        // first four bytes are the size of the tag string
        val tagSize = ByteBuffer.wrap(bytes, 0, 4).int

        // next tagSize bytes are the tag
        val tagBytes = ByteArray(tagSize)
        ByteBuffer.wrap(bytes, 4, tagSize).get(tagBytes)

        return tagBytes
    }

    fun idFromBytes(bytes: ByteArray): Int {
        // first four bytes are the size of the tag string
        val tagSize = ByteBuffer.wrap(bytes, 0, 4).getInt()

        // next four bytes after tag are the id
        return ByteBuffer.wrap(bytes, 4 + tagSize, 4).getInt()
    }

    fun messageFromBytes(bytes: ByteArray): ByteArray {
        // first four bytes are the size of the tag string
        val tagSize = ByteBuffer.wrap(bytes, 0, 4).getInt()

        val messageOffset = 4 + tagSize + 4
        val messageSize = bytes.size - messageOffset

        // next messageSize bytes are the message
        return bytes.copyOfRange(messageOffset, messageOffset + messageSize)
    }

    fun messageFromBytes(bytes: ByteArray, target: ByteArray) {
        // first four bytes are the size of the tag string
        val tagSize = ByteBuffer.wrap(bytes, 0, 4).getInt()

        val messageOffset = 4 + tagSize + 4
        val messageSize = bytes.size - messageOffset

        if(target.size < messageSize) {
            throw IllegalArgumentException("Target array is too small to fit the message")
        }

        // next messageSize bytes are the message
        System.arraycopy(bytes, messageOffset, target, 0, messageSize)
    }

    fun toBytes(tag: ByteArray, id: Int, message: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(4 + tag.size + 4 + message.size)
        buffer.putInt(tag.size)
        buffer.put(tag)
        buffer.putInt(id)
        buffer.put(message)

        return buffer.array()
    }

    fun messageLengthFromBytes(it: ByteArray) = it.size - messageOffsetFromBytes(it)

    fun messageOffsetFromBytes(it: ByteArray) = 4 + tagFromBytes(it).size + 4
}