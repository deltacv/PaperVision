package io.github.deltacv.papervision.platform.lwjgl.glfw

import imgui.ImVec2
import io.github.deltacv.papervision.platform.lwjgl.util.loadImageFromResource
import io.github.deltacv.papervision.platform.PlatformWindow
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.stb.STBImage.stbi_image_free
import java.nio.Buffer

class GlfwWindow(val ptrSupplier: () -> Long) : PlatformWindow {

    override var title: String = ""
        set(value) {
            field = value
            glfwSetWindowTitle(ptrSupplier(), title)
        }
    override var icon: String = ""
        set(value) {
            val image = loadImageFromResource(value)

            GLFWImage.malloc(1).use {
                it.position(0)
                    .width(image.width)
                    .height(image.height)
                    .pixels(image.buffer)

                it.position(0)
                glfwSetWindowIcon(ptrSupplier(), it)

                stbi_image_free(image.buffer)
            }

            field = value
        }

    private val w = BufferUtils.createIntBuffer(1)
    private val h = BufferUtils.createIntBuffer(1)

    internal var cachedSize: ImVec2? = null

    override val size: ImVec2
        get() {
            if(cachedSize != null) return cachedSize!!

            (w as Buffer).position(0)
            (h as Buffer).position(0)

            glfwGetWindowSize(ptrSupplier(), w, h)

            cachedSize = ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
            return cachedSize!!
        }

    override var visible: Boolean
        get() = glfwGetWindowAttrib(ptrSupplier(), GLFW_VISIBLE) == GLFW_TRUE
        set(value) {
            if(value) {
                glfwShowWindow(ptrSupplier())
            } else {
                glfwHideWindow(ptrSupplier())
            }
        }

}