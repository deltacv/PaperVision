package io.github.deltacv.easyvision.platform

import java.nio.ByteBuffer

interface PlatformTexture {

    val width: Int
    val height: Int

    val id: Int

    fun set(bytes: ByteArray)

    fun delete()

}

interface PlatformTextureFactory {
    fun create(width: Int, height: Int, bytes: ByteArray): PlatformTexture
}