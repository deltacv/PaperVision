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

package io.github.deltacv.papervision.plugin.ipc.eocvsim

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.visionloop.receiver.MjpegHttpStreamerReceiver
import io.javalin.http.Handler
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class EOCVSimEngineImageStreamer(
    val resolution: Size
) : ImageStreamer {

    private val handlers = mutableMapOf<Int, Handler>()
    val receivers = mutableMapOf<Int, MjpegHttpStreamerReceiver>()

    val logger by loggerForThis()

    private val tempMat = Mat()

    override fun sendFrame(
        id: Int,
        image: Mat,
        cvtCode: Int?
    ) {
        if (!receivers.containsKey(id)) {
            val receiver = MjpegHttpStreamerReceiver(0, resolution)
            handlers[id] = receiver.takeHandler() // save handler for later

            logger.info("Creating new Mjpeg stream for id $id")

            receiver.init(emptyArray())
            receivers[id] = receiver
        }

        val receiver = receivers[id]!!

        if (cvtCode != null) {
            // convert image to the desired color space
            Imgproc.cvtColor(image, tempMat, cvtCode)
            receiver.take(tempMat)
        } else {
            receiver.take(image)
        }
    }

    /**
     * Get the handler for a specific id
     * Used to attach the handler to a Javalin instance
     * @param id the id of the stream
     */
    fun handlerFor(id: Int) = handlers[id]

    fun handlers() = handlers.toMap()

    fun stop() {
        logger.info("Stopping EOCVSimEngineImageStreamer")

        for ((_, receiver) in receivers) {
            receiver.close()
        }
    }
}