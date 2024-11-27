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

package io.github.deltacv.papervision.platform.animation

import io.github.deltacv.papervision.platform.PlatformTexture
import io.github.deltacv.papervision.util.ElapsedTime
import java.nio.ByteBuffer

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

    override val textureId: Long get() = textures[frame].textureId

    override fun delete() {
        for(texture in textures) {
            texture.delete()
        }
        isActive = false
    }

}