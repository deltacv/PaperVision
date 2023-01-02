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
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.codegen.CodeGenManager
import io.github.deltacv.easyvision.codegen.language.interpreted.JythonLanguage
import io.github.deltacv.easyvision.gui.*
import io.github.deltacv.easyvision.gui.style.imnodes.ImNodesDarkStyle
import io.github.deltacv.easyvision.gui.util.Popup
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.id.IdElementContainerStack
import io.github.deltacv.easyvision.id.NoneIdElement
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.io.resourceToString
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.node.NodeScanner
import io.github.deltacv.easyvision.platform.*
import io.github.deltacv.easyvision.serialization.ev.EasyVisionSerializer
import io.github.deltacv.easyvision.util.eocvsim.EOCVSimIpcManager
import io.github.deltacv.easyvision.util.event.EventHandler
import io.github.deltacv.easyvision.util.loggerForThis
import io.github.deltacv.mai18n.Language

class EasyVision(private val setupCall: PlatformSetupCallback) {

    companion object {
        lateinit var platformKeys: PlatformKeys
            private set

        var imnodesStyle = ImNodesDarkStyle

        lateinit var defaultImGuiFont: Font
            private set

        val miscIds = IdElementContainer<NoneIdElement>()
    }

    val logger by loggerForThis()

    lateinit var setup: PlatformSetup
        private set
    lateinit var window: PlatformWindow
        private set
    lateinit var textureFactory: PlatformTextureFactory
        private set

    val onUpdate = EventHandler("EasyVision-OnUpdate")

    val keyManager = KeyManager()
    val codeGenManager = CodeGenManager(this)
    val fontManager = FontManager()

    val langManager = Language("/lang.csv", "en").makeTr()

    val nodeEditor by lazy { NodeEditor(this, keyManager) }
    val nodeList by lazy { NodeList(this, keyManager) }

    val eocvSimIpc = EOCVSimIpcManager(this)

    val nodes = IdElementContainer<Node<*>>()
    val attributes = IdElementContainer<Attribute>()
    val links = IdElementContainer<Link>()

    lateinit var defaultFont: Font
        private set

    fun init() {
        IdElementContainerStack.threadStack.push(nodes)
        IdElementContainerStack.threadStack.push(attributes)
        IdElementContainerStack.threadStack.push(links)

        EasyVisionSerializer.deserializeAndApply(resourceToString("/testproj.json"), this)

        logger.info("Starting EasyVision...")

        eocvSimIpc.init()

        NodeScanner.startAsyncScan()

        logger.info("Using the ${setupCall.name} platform")
        setup = setupCall.setup()

        platformKeys = setup.keys ?: throw IllegalArgumentException("Platform ${setup.name} must provide PlatformKeys")
        window = setup.window ?: throw IllegalArgumentException("Platform ${setup.name} must provide a Window")
        textureFactory = setup.textureFactory ?: throw IllegalArgumentException("Platform ${setup.name} must provide a TextureFactory")

        // disable annoying ini file creation (hopefully shouldn't break anything)
        ImGui.getIO().iniFilename = null
        ImGui.getIO().logFilename = null

        // initializing fonts right after the imgui context is created
        // we can't create fonts mid-frame so that's kind of a problem
        defaultFont = fontManager.makeFont("/fonts/Calcutta-Regular.otf", 13f)
        defaultImGuiFont = fontManager.makeDefaultFont(13f)

        nodeEditor.enable()
        langManager.loadIfNeeded()

        nodeList.enable()

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()
        IdElementContainerStack.threadStack.pop<Link>()
    }

    fun firstProcess() {
        window.title = "EasyVision"
        window.icon = "/ico/ico_ezv.png"
    }

    fun process() {
        IdElementContainerStack.threadStack.push(nodes)
        IdElementContainerStack.threadStack.push(attributes)
        IdElementContainerStack.threadStack.push(links)

        onUpdate.run()

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = window.size
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        for(window in Window.windows.inmutable) {
            window.draw()
        }
        for(tooltip in Popup.popups.inmutable) {
            tooltip.draw()
        }

        ImGui.popFont()

        if(keyManager.pressed(Keys.ArrowUp)) {
            println(EasyVisionSerializer.serialize(nodes.elements, links.elements))
        }

        if(keyManager.pressed(Keys.Escape)) {
            println(codeGenManager.build("test"))
        }

        keyManager.update()

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()
        IdElementContainerStack.threadStack.pop<Link>()
    }

    fun destroy() {
        nodeEditor.destroy()
        eocvSimIpc.stop()
    }
}