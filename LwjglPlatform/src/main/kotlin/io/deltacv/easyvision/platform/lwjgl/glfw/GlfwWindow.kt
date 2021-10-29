package io.deltacv.easyvision.platform.lwjgl.glfw

import imgui.ImVec2
import io.github.deltacv.easyvision.platform.PlatformWindow
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*

class GlfwWindow(val ptrSupplier: () -> Long) : PlatformWindow {

    override var title: String = ""
        set(value) {
            field = value
            glfwSetWindowTitle(ptrSupplier(), title)
        }

    private val w = BufferUtils.createIntBuffer(1)
    private val h = BufferUtils.createIntBuffer(1)

    internal var cachedSize: ImVec2? = null

    override val size: ImVec2
        get() {
            if(cachedSize != null) return cachedSize!!

            w.position(0)
            h.position(0)

            glfwGetWindowSize(ptrSupplier(), w, h)

            cachedSize = ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
            return cachedSize!!
        }

}