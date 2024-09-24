package io.github.deltacv.papervision.plugin

import com.google.gson.JsonElement
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.serialization.PaperVisionSerializer
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.event.EventListener

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

    private lateinit var app: PaperVisionApp
    val paperVision get() = app.paperVision

    val onAppInstantiate = PaperVisionEventHandler("PaperVisionDaemon-OnAppInstantiate")

    fun launchDaemonPaperVision(instantiator: () -> PaperVisionApp) {
        app = instantiator()

        onAppInstantiate.run()
        onAppInstantiate.callRightAway = true

        require(app.eventLoopHandler != null) {
            "PaperVisionApp handler must not be null"
        }

        app.start()
    }

    fun openProject(json: JsonElement) {
        paperVision.onUpdate.doOnce {
            PaperVisionSerializer.deserializeAndApply(json, paperVision)

            if(!app.glfwWindow.visible) {
                app.glfwWindow.visible = true
                app.glfwWindow.maximized = true
                app.glfwWindow.focus = true
            }
        }
    }

    fun currentProjectJson() = PaperVisionSerializer.serialize(
        paperVision.nodes.inmutable, paperVision.links.inmutable
    )

    fun currentProjectJsonTree() = PaperVisionSerializer.serializeToTree(
        paperVision.nodes.inmutable, paperVision.links.inmutable
    )

    fun showPaperVision() {
        app.glfwWindow.visible = true
    }

    fun hidePaperVision() {
        app.glfwWindow.visible = false
    }

    fun invokeOnMainLoop(runnable: Runnable) =
        paperVision.onUpdate.doOnce(runnable)

    fun attachToMainLoop(listener: EventListener) =
        paperVision.onUpdate(listener)

    fun attachToEditorChange(listener: EventListener) =
        paperVision.nodeEditor.onEditorChange(listener)
}