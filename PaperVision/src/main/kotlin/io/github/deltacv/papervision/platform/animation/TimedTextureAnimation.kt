package io.github.deltacv.papervision.platform.animation

import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.util.ElapsedTime

class TimedTextureAnimation(
    val fps: Double,
    val textures: Array<PlatformTexture>
): PlatformTextureAnimation() {

    override var frame = 0
    private var halfFrames = 0.0

    override var isActive = true

    private val deltaTimer = ElapsedTime()

    override fun next() {
        frame += 1

        if(frame >= textures.size) {
            frame = 0
        }
    }

    override fun update() {
        if(isActive) {
            halfFrames += fps * deltaTimer.seconds

            frame = halfFrames.toInt()

            if(frame >= textures.size) {
                halfFrames -= frame
                frame = 0
            }
        }

        deltaTimer.reset()
    }

    override val width: Int get() = textures[frame].width
    override val height: Int get() = textures[frame].height

    override val textureId: Int get() = textures[frame].textureId

    override fun delete() {
        for(texture in textures) {
            texture.delete()
        }
        isActive = false
    }

}