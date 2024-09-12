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

        // next four bytes are the id
        return ByteBuffer.wrap(bytes, 4 + tagSize, 4).getInt()
    }

    fun messageFromBytes(bytes: ByteArray): ByteArray {
        // first four bytes are the size of the tag string
        val tagSize = ByteBuffer.wrap(bytes, 0, 4).getInt()

        val messageOffset = 4 + tagSize + 4
        val messageSize = bytes.size - messageOffset

        // next messageSize bytes are the message
        return ByteBuffer.wrap(bytes, messageOffset, messageSize).array()
    }

    fun toBytes(tag: ByteArray, id: Int, message: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(4 + tag.size + 4 + message.size)
        buffer.putInt(tag.size)
        buffer.put(tag)
        buffer.putInt(id)
        buffer.put(message)

        return buffer.array()
    }

}