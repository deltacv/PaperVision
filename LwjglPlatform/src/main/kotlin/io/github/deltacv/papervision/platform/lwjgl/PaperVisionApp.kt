package io.github.deltacv.papervision.platform.lwjgl

import imgui.app.Configuration
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.engine.bridge.PaperVisionEngineBridge
import io.github.deltacv.papervision.io.KeyAction
import io.github.deltacv.papervision.platform.lwjgl.glfw.GlfwKeys
import io.github.deltacv.papervision.platform.lwjgl.glfw.GlfwWindow
import io.github.deltacv.papervision.platform.lwjgl.texture.OpenGLTextureFactory
import io.github.deltacv.papervision.platform.platformSetup
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback

class PaperVisionApp @JvmOverloads constructor(
    val daemon: Boolean,
    val bridge: PaperVisionEngineBridge? = null,
    val windowCloseListener: (() -> Boolean)? = null
) : EventLoopWindow() {

    val setup = platformSetup("LWJGL") {
        window = glfwWindow
        textureFactory = OpenGLTextureFactory
        keys = GlfwKeys
        engineBridge = bridge
    }

    val glfwWindow = GlfwWindow { handle }

    val paperVision = PaperVision(setup)

    fun start() {
        val config = Configuration().apply {
            title = ""
            isFullScreen = true
        }

        init(config, daemon)

        while(run());

        postRun()
        dispose()
    }

    override fun initImGui(config: Configuration) {
        super.initImGui(config)

        paperVision.init()
    }

    private var hasProcessed = false
    private var prevKeyCallback: GLFWKeyCallback? = null

    override fun process() {
        if(!hasProcessed) {
            paperVision.firstProcess()
            hasProcessed = true
        }

        if (prevKeyCallback == null) {
            // register a new key callback that will call the previous callback and handle some special keys
            prevKeyCallback = glfwSetKeyCallback(handle, ::keyCallback)
        }
        glfwWindow.cachedSize = null

        paperVision.process()
    }

    fun postRun() {
        paperVision.destroy()
    }

    override fun windowCloseAction(): Boolean {
        val shouldHide = windowCloseListener?.invoke() ?: true

        if(daemon) {
            if(shouldHide) {
                glfwWindow.visible = false
            }
            glfwSetWindowShouldClose(handle, false)
            return false
        } else {
            return shouldHide
        }
    }

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        // thanks.
        paperVision.keyManager.updateKey(scancode, when(action) { // mapping the glfw action Int to a KeyAction
            GLFW_PRESS -> KeyAction.PRESS
            GLFW_REPEAT -> KeyAction.PRESSING
            GLFW_RELEASE -> KeyAction.RELEASE
            else -> KeyAction.UNKNOWN
        })
    }
}