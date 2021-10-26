/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.easyvision

import com.github.serivesmejia.eocvsim.util.Log
import imgui.ImGui
import imgui.ImVec2
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.codegen.CodeGenManager
import io.github.deltacv.easyvision.gui.Font
import io.github.deltacv.easyvision.gui.FontManager
import io.github.deltacv.easyvision.gui.NodeEditor
import io.github.deltacv.easyvision.gui.NodeList
import io.github.deltacv.easyvision.gui.style.imnodes.ImNodesDarkStyle
import io.github.deltacv.easyvision.gui.util.PopupBuilder
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.mai18n.LangManager
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFWKeyCallback

class EasyVision : Application() {

    companion object {
        const val TAG = "EasyVision"

        var imnodesStyle = ImNodesDarkStyle
        lateinit var defaultImGuiFont: Font
            private set

        private var ptr = 0L

        private val w = BufferUtils.createIntBuffer(1)
        private val h = BufferUtils.createIntBuffer(1)

        val windowSize: ImVec2 get() {
            w.position(0)
            h.position(0)

            glfwGetWindowSize(ptr, w, h)

            return ImVec2(w.get(0).toFloat(), h.get(0).toFloat())
        }

        val miscIds = IdElementContainer<Any>()
    }

    private var prevKeyCallback: GLFWKeyCallback? = null

    val keyManager = KeyManager()
    val codeGenManager = CodeGenManager(this)
    val fontManager = FontManager()

    val langManager = LangManager("/lang.csv", "en").makeTr()

    val nodeEditor = NodeEditor(this, keyManager)
    val nodeList = NodeList(this, keyManager)

    lateinit var defaultFont: Font
        private set

    fun start() {
        Log.info(TAG, "Starting EasyVision...")
        Log.blank()

        nodeEditor.init()
        langManager.loadIfNeeded()

        launch(this)

        nodeEditor.destroy()
    }

    override fun configure(config: Configuration) {
        config.title = "EasyVision"
    }

    override fun initImGui(config: Configuration?) {
        super.initImGui(config)

        // initializing fonts right after the imgui context is created
        // we can't create fonts mid-frame so that's kind of a problem
        defaultFont = fontManager.makeFont("/fonts/Calcutta-Regular.otf", "Calcutta", 13f)
        defaultImGuiFont = fontManager.makeDefaultFont(13f)

        nodeList.init()
    }

    override fun process() {
        if(prevKeyCallback == null) {
            ptr = handle
            // register a new key callback that will call the previous callback and handle some special keys
            prevKeyCallback = glfwSetKeyCallback(handle, ::keyCallback)
        }

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = windowSize
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                    or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoBringToFrontOnFocus
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration
        )

        nodeEditor.draw()

        ImGui.end()
        ImGui.popFont()

        nodeList.draw()

        ImGui.pushFont(defaultFont.imfont)
        PopupBuilder.draw()
        ImGui.popFont()

        keyManager.update()

        if(ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            codeGenManager.build()
        }
    }

    private fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if(prevKeyCallback != null) {
            prevKeyCallback!!.invoke(windowId, key, scancode, action, mods) //invoke the imgui callback
        }

        // thanks.
        keyManager.updateKey(scancode, action)
    }
}

fun main() {
    EasyVision().start()
}