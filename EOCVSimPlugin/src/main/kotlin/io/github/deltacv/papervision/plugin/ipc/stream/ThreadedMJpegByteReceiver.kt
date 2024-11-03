package io.github.deltacv.papervision.plugin.ipc.stream

import io.github.deltacv.papervision.engine.client.ByteMessageReceiver
import io.github.deltacv.papervision.util.hexString
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.net.URL

class ThreadedMJpegByteReceiver(
    val tagProvider: () -> String,
    val url: String
) : ByteMessageReceiver() {

    private val threads = mutableMapOf<Int, Thread>()

    private var previousTag = ""

    private val availableChecker = Runnable {
        while(!Thread.interrupted()) {
            val tag = tagProvider()

            if(previousTag != tag) {
                logger.info("Tag $previousTag is different from current tag ${tag}")
            }

            previousTag = tag

            val available = IOUtils.toString(URL("${url.trimEnd('/')}/available"), Charsets.UTF_8)

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
                        val stream = MJpegHttpReader("${url.trimEnd('/')}/$id")
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