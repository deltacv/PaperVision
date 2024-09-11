package io.github.deltacv.papervision.plugin

import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.util.event.EventListener
import java.util.concurrent.Executors

/**
 * Manages and executes the PaperVision process in a dedicated daemon thread.
 *
 * This object is responsible for starting PaperVision within a static daemon thread, allowing it
 * to run in the background without blocking the main thread. The daemon thread will remain active
 * as long as PaperVision is running and will exit when PaperVision stops or the application shuts down.
 *
 * It is expected that the PaperVision process will be initialized and controlled through this object.
 */
object PaperVisionDaemon {

    private val executorService = Executors.newFixedThreadPool(2)

    private val app by lazy { PaperVisionApp(true) }
    val paperVision get() = app.paperVision

    val future by lazy {
        executorService.submit {
            app.start()
        }
    }

    fun launchDaemonPaperVision() = future

    fun invokeOnMainLoop(runnable: Runnable) =
        paperVision.onUpdate.doOnce(runnable)

    fun attachToMainLoop(listener: EventListener) =
        paperVision.onUpdate(listener)

    fun invokeLater(runnable: Runnable) = executorService.submit(runnable)

    /**
     * Stops the PaperVision process by shutting down the executor service.
     */
    fun stop() {
        future?.cancel(true)
    }
}