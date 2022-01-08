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
 */

package io.github.deltacv.easyvision

import imgui.ImGui
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
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.io.resourceToString
import io.github.deltacv.easyvision.node.NodeScanner
import io.github.deltacv.easyvision.platform.PlatformKeys
import io.github.deltacv.easyvision.platform.PlatformSetup
import io.github.deltacv.easyvision.platform.PlatformSetupCallback
import io.github.deltacv.easyvision.platform.PlatformWindow
import io.github.deltacv.easyvision.serialization.ev.EasyVisionSerializer
import io.github.deltacv.easyvision.util.IpcClientWatchDog
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.eocvsim.ipc.IpcClient
import io.github.deltacv.mai18n.LangManager

class EasyVision(private val setupCall: PlatformSetupCallback) {

    companion object {
        lateinit var platformKeys: PlatformKeys
            private set

        var imnodesStyle = ImNodesDarkStyle

        lateinit var defaultImGuiFont: Font
            private set

        val miscIds = IdElementContainer<Any>()
    }

    val logger by loggerForThis()

    lateinit var setup: PlatformSetup
        private set
    lateinit var window: PlatformWindow
        private set

    val keyManager = KeyManager()
    val codeGenManager = CodeGenManager(this)
    val fontManager = FontManager()

    val langManager = LangManager("/lang.csv", "en").makeTr()

    val nodeEditor = NodeEditor(this, keyManager)
    val nodeList = NodeList(this, keyManager)

    val eocvSimIpcClient = IpcClientWatchDog()

    lateinit var defaultFont: Font
        private set

    fun init() {
        EasyVisionSerializer.deserializeAndApply(resourceToString("/testproj.json"), nodeEditor)

        logger.info("Starting EasyVision...")

        eocvSimIpcClient.start()

        NodeScanner.startAsyncScan()

        logger.info("Using the ${setupCall.name} platform")
        setup = setupCall.setup()

        platformKeys = setup.keys ?: throw IllegalArgumentException("Platform ${setup.name} must provide PlatformKeys")
        window = setup.window ?: throw IllegalArgumentException("Platform ${setup.name} must provide a Window")

        // disable annoying ini file creation (hopefully shouldn't break anything)
        ImGui.getIO().iniFilename = null
        ImGui.getIO().logFilename = null

        nodeEditor.init()
        langManager.loadIfNeeded()

        // initializing fonts right after the imgui context is created
        // we can't create fonts mid-frame so that's kind of a problem
        defaultFont = fontManager.makeFont("/fonts/Calcutta-Regular.otf", "Calcutta", 13f)
        defaultImGuiFont = fontManager.makeDefaultFont(13f)

        nodeList.init()
    }

    fun firstProcess() {
        window.title = "EasyVision"
        window.icon = "/ico/ico_ezv.png"
    }

    fun process() {
        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = window.size
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

        if(ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            codeGenManager.build()
        }

        if(keyManager.pressed(Keys.ArrowUp)) {
            println(EasyVisionSerializer.serializeCurrent())
        }

        keyManager.update()
    }

    fun destroy() {
        nodeEditor.destroy()
        eocvSimIpcClient.stop()
    }
}