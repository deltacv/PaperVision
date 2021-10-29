package io.deltacv.easyvision.platform.lwjgl

import imgui.app.Application
import imgui.app.Configuration
import io.deltacv.easyvision.platform.lwjgl.glfw.GlfwKeys
import io.deltacv.easyvision.platform.lwjgl.glfw.GlfwWindow
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.io.KeyAction
import io.github.deltacv.easyvision.platform.platformSetup
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback

class EasyVisionApp : Application() {

    val setup = platformSetup("LWJGL") {
        window = glfwWindow
        keys = GlfwKeys
    }

    val glfwWindow = GlfwWindow { handle }
    val easyVision = EasyVision(setup)

    fun start() {
        launch(this)
        easyVision.destroy() // launch won't return until the app is closed
    }

    override fun initImGui(config: Configuration) {
        super.initImGui(config)

        easyVision.init()
    }

    private var hasProcessed = false
    private var prevKeyCallback: GLFWKeyCallback? = null

    override fun process() {
        if(!hasProcessed) {
            easyVision.firstProcess()
            hasProcessed = true
        }

        if (prevKeyCallback == null) {
            // register a new key callback that will call the previous callback and handle some special keys
            prevKeyCallback = glfwSetKeyCallback(handle, ::keyCallback)
        }
        glfwWindow.cachedSize = null

        easyVision.process()
    }

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        // thanks.
        easyVision.keyManager.updateKey(scancode, when(action) { // mapping the glfw action Int to a KeyAction
            GLFW_PRESS -> KeyAction.PRESS
            GLFW_REPEAT -> KeyAction.PRESSING
            GLFW_RELEASE -> KeyAction.RELEASE
            else -> KeyAction.UNKNOWN
        })
    }
}