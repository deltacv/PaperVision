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

package io.github.deltacv.papervision.plugin.ipc.stream

import io.github.deltacv.papervision.engine.client.ByteMessageReceiver
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.net.URL

class ThreadedMjpegByteReceiver(
    val tagProvider: () -> String,
    val url: String
) : ByteMessageReceiver() {

    private val threads = mutableMapOf<Int, Thread>()

    private var previousTag = ""

    private val availableChecker = Runnable {
        val availableUrl = URL("${url.trimEnd('/')}/available")

        while(!Thread.interrupted()) {
            val tag = tagProvider()

            if(previousTag != tag) {
                logger.info("Tag $previousTag is different from current tag {}", tag)
            }

            previousTag = tag

            val available = availableUrl.readText()
            if(available.isBlank()) {
                continue
            }

            val ids = try {
                available.split(",").map { it.toInt() }
            } catch(e: Exception) {
                logger.error("Error parsing available ids from $available", e)
                return@Runnable
            }

            for(id in ids) {
                if(!threads.contains(id)) {
                    val thread = Thread({
                        val stream = MjpegHttpReader("${url.trimEnd('/')}/$id")
                        stream.start()

                        for(frame in stream) {
                            callHandlers(id, tagProvider(), frame)
                        }

                        logger.info("MJpegHttpReader thread for id $id ended")
                    }, "MJpegHttpReader-$id")

                    logger.info("Starting MJpegHttpReader thread for id $id")

                    thread.start()
                    threads[id] = thread
                }
            }

            try {
                Thread.sleep(500)
            } catch(_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("Starting ThreadedMJpegByteReceiver for ${tagProvider()}")

        threads[-1] = Thread(availableChecker, "MJpegHttpReader-AvailableChecker-${tagProvider()}").apply { start() }
    }

    override fun stop() {
        logger.info("Stopping ThreadedMJpegByteReceiver for ${tagProvider()}")

        for((_, thread) in threads) {
            thread.interrupt()
        }
    }

}