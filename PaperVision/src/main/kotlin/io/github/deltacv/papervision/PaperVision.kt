/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision

import imgui.ImFontGlyphRangesBuilder
import imgui.ImGui
import imgui.flag.ImGuiCond
import io.github.deltacv.papervision.action.Action
import io.github.deltacv.papervision.action.RootAction
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.CodeGenManager
import io.github.deltacv.papervision.engine.bridge.NoOpPaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.message.PrevizAskNameMessage
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.engine.previz.ClientPrevizManager
import io.github.deltacv.papervision.gui.*
import io.github.deltacv.papervision.gui.display.ImageDisplay
import io.github.deltacv.papervision.gui.editor.IntroModalWindow
import io.github.deltacv.papervision.gui.editor.NodeEditor
import io.github.deltacv.papervision.gui.editor.NodeList
import io.github.deltacv.papervision.gui.style.CurrentStyles
import io.github.deltacv.papervision.gui.style.imnodes.ImNodesDarkStyle
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.gui.util.FontManager
import io.github.deltacv.papervision.gui.Popup
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.util.defaultFontConfig
import io.github.deltacv.papervision.id.*
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.NodeRegistry
import io.github.deltacv.papervision.platform.*
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis
import org.deltacv.mai18n.Language
import org.deltacv.mai18n.makeThreadTr
import java.awt.Taskbar
import java.awt.Toolkit

class PaperVision(
    private val setupCall: PlatformSetupCallback
) {

    companion object {
        var imnodesStyle
            set(value) {
                CurrentStyles.imnodesStyle = value
            }
            get() = CurrentStyles.imnodesStyle
        init {
            imnodesStyle = ImNodesDarkStyle
        }
    }

    private val logger by loggerForThis()

    lateinit var setup: PlatformSetup
        private set
    lateinit var window: PlatformWindow
        private set
    lateinit var textureFactory: PlatformTextureFactory
        private set
    lateinit var config: PlatformConfig
        private set

    val onInit            = PaperVisionEventHandler("PaperVision-OnInit")
    val onUpdate          = PaperVisionEventHandler("PaperVision-OnUpdate")
    val onDeserialization = PaperVisionEventHandler("PaperVision-OnDeserialization")

    // these depend on PlatformSetup so they are initialized later
    lateinit var textureProcessorQueue: TextureProcessorQueue
        private set
    lateinit var keyManager: KeyManager
        private set

    val codeGenManager  = CodeGenManager(this)
    val fontManager     = FontManager()
    lateinit var currentLanguage: Language
        private set

    val nodeEditor by lazy { NodeEditor(this, keyManager) }
    val nodeList   by lazy { NodeList(this, keyManager, NodeRegistry.nodes) }

    val nodes                  = IdContainer<Node<*>>()
    val attributes             = IdContainer<Attribute>()
    val links                  = IdContainer<Link>()
    val windows                = IdContainer<Window>()
    val textures               = IdContainer<PlatformTexture>()
    val textureProcessorQueues = SingleIdContainer<TextureProcessorQueue>()
    val fonts                  = IdContainer<Font>()
    val streamDisplays         = IdContainer<ImageDisplay>()
    val actions                = IdContainer<Action>()
    val misc                   = IdContainer<Misc>()
    val popups                 = IdContainer<Popup>()

    val isModalWindowOpen get() = windows.inmutable.any { it.isModal && it.isVisible }

    lateinit var engineClient: PaperVisionEngineClient
    lateinit var previzManager: ClientPrevizManager

    lateinit var defaultFont: Font

    /** Helper to simplify font creation */
    private fun font(name: String, path: String, size: Float) =
        fontManager.makeFont(name, path, defaultFontConfig(size))

    /** Executes a block of code with all containers pushed/popped safely */
    private inline fun withStacks(block: () -> Unit) {
        IdContainerStacks.local.push(nodes)
        IdContainerStacks.local.push(attributes)
        IdContainerStacks.local.push(links)
        IdContainerStacks.local.push(windows)
        IdContainerStacks.local.push(textures)
        IdContainerStacks.local.push(textureProcessorQueues)
        IdContainerStacks.local.push(fonts)
        IdContainerStacks.local.push(streamDisplays)
        IdContainerStacks.local.push(actions)
        IdContainerStacks.local.push(popups)
        IdContainerStacks.local.push(misc)

        try {
            block()
        } finally {
            IdContainerStacks.local.pop<Node<*>>()
            IdContainerStacks.local.pop<Attribute>()
            IdContainerStacks.local.pop<Link>()
            IdContainerStacks.local.pop<Window>()
            IdContainerStacks.local.pop<PlatformTexture>()
            IdContainerStacks.local.pop<TextureProcessorQueue>()
            IdContainerStacks.local.pop<Font>()
            IdContainerStacks.local.pop<ImageDisplay>()
            IdContainerStacks.local.pop<Action>()
            IdContainerStacks.local.pop<Popup>()
            IdContainerStacks.local.pop<Misc>()
        }
    }

    fun init() = withStacks {
        logger.info("Starting PaperVision...\n\n${IntroModalWindow.iconLogo}\n")
        logger.info("Using the ${setupCall.name} platform")

        initPlatform()
        initLanguage()
        initEngine()
        initFonts()
        initUI()

        RootAction().enable()
        onInit.run()

        setTaskbarIcon()

        dumpStartupDiagnostics()
        logger.info("PaperVision started")
    }

    private fun initPlatform() {
        setup = setupCall.setup()
        keyManager = KeyManager(setup.keys ?: error("Platform ${setup.name} must provide PlatformKeys"))
        window = setup.window ?: error("Platform ${setup.name} must provide a Window")
        textureFactory = setup.textureFactory ?: error("Platform ${setup.name} must provide a TextureFactory")
        config = setup.config
        config.load()
    }

    private fun initLanguage() {
        try {
            changeLanguage(config.fields.lang)
        } catch (_: Exception) {
            logger.warn("Configured language ${config.fields.lang} is not available, defaulting")
        }
    }

    private fun initEngine() {
        textureProcessorQueue = TextureProcessorQueue(textureFactory)
        textureProcessorQueue.enable()

        engineClient = PaperVisionEngineClient(setup.engineBridge ?: NoOpPaperVisionEngineBridge)
        previzManager = ClientPrevizManager(
            160, 120, codeGenManager, engineClient, setup.previzByteMessageReceiverProvider
        )
        engineClient.connect()

        ImGui.getIO().apply {
            iniFilename = null
            logFilename = null
            fonts.setFreeTypeRenderer(false)
        }
    }

    private fun initFonts() {
        val rangesBuilder = ImFontGlyphRangesBuilder().apply {
            addRanges(ImGui.getIO().fonts.glyphRangesDefault)
            addRanges(FontAwesomeIcons._IconRange)
        }
        val ranges = rangesBuilder.buildRanges()

        defaultFont = font("calcutta", "/fonts/Calcutta-SemiBold.otf", 20f)
        font("calcutta-big", "/fonts/Calcutta-SemiBold.otf", 28f)
        font("jetbrains-mono", "/fonts/JetBrainsMono-Regular.ttf", 28f)

        fontManager.makeDefaultFont(20) // "default-20"
        fontManager.makeDefaultFont(12) // "default-12"

        // Pass the ranges to the icon fonts so FontAwesome glyphs are available
        fontManager.makeFont("font-awesome", "/fonts/icons/FontAwesome6-Free-Solid-900.otf", defaultFontConfig(16f), ranges)
        fontManager.makeFont("font-awesome-big", "/fonts/icons/FontAwesome6-Free-Solid-900.otf", defaultFontConfig(52f), ranges)
        fontManager.makeFont("font-awesome-brands", "/fonts/icons/FontAwesome6-Brands-Regular-400.otf", defaultFontConfig(16f), ranges)
        fontManager.makeFont("font-awesome-brands-big", "/fonts/icons/FontAwesome6-Brands-Regular-400.otf", defaultFontConfig(80f), ranges)
    }

    private fun initUI() {
        nodeEditor.enable()
        nodeList.enable()
    }

    private fun dumpStartupDiagnostics() {
        logger.debug("=== PaperVision JVM startup diagnostics ===")

        logger.debug("java.version = {}", System.getProperty("java.version"))
        logger.debug("java.home = {}", System.getProperty("java.home"))
        logger.debug("java.class.path = {}", System.getProperty("java.class.path"))
        logger.debug("java.library.path = {}", System.getProperty("java.library.path"))
        logger.debug("org.lwjgl.librarypath = {}", System.getProperty("org.lwjgl.librarypath"))
        logger.debug("java.io.tmpdir = {}", System.getProperty("java.io.tmpdir"))
        logger.debug("user.dir = {}", System.getProperty("user.dir"))

        // env vars
        val path = System.getenv("PATH") ?: "<null>"
        val temp = System.getenv("TEMP") ?: System.getenv("TMP") ?: "<null>"

        logger.debug("env PATH (truncated) = {}", path.take(800))
        logger.debug("env TEMP/TMP = {}", temp)

        logger.debug("=== end diagnostics ===")
    }

    private fun setTaskbarIcon() {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().iconImage = Toolkit.getDefaultToolkit().getImage(
                javaClass.getResource("/ico/ico_ezv.png")
            )
        } else {
            logger.warn("Taskbar icon not supported")
        }
    }

    fun firstProcess() {
        window.title = "PaperVision"
        window.icon = "/ico/ico_ezv.png"
        window.maximized = true

        onUpdate.doOnce {
            if (setup.showWelcomeWindow) {
                showWelcome()
            } else {
                onDeserialization {
                    if (nodeEditor.flags.getOrElse("showWelcome") { true }) showWelcome()
                }
            }
            window.requestFocus()
        }
    }

    fun showWelcome(askLanguage: Boolean = setup.config.fields.shouldAskForLang) {
        IntroModalWindow(
            nodeEditor, chooseLanguage = askLanguage
        ).apply {
            onDontShowAgain {
                nodeEditor.flags["showWelcome"] = false
                logger.info("showWelcome = ${nodeEditor.flags["showWelcome"]}")
            }
        }.enable()
    }

    fun process() = withStacks {
        onUpdate.run()
        engineClient.process()

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)
        val size = window.size
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        windows.inmutable.forEach { it.draw() }
        popups.inmutable.forEach { it.draw() }

        textureProcessorQueues.inmutable.forEach { it.draw() }

        ImGui.popFont()

        keyManager.update()
        previzManager.update()
    }

    fun destroy() {
        logger.info("Shutting down PaperVision...")

        config.save()

        engineClient.disconnect()

        textureProcessorQueue.delete()

        windows.inmutable.reversed().forEach { it.delete() }
        popups.inmutable.reversed().forEach { it.delete() }

        nodeEditor.delete()
        nodeList.delete()
    }

    fun changeLanguage(langCode: String) {
        currentLanguage = Language("/lang_pv.csv", langCode).apply { makeThreadTr() }
    }

    fun startPrevizWithEngine() {
        engineClient.sendMessage(PrevizAskNameMessage().onResponseWith<StringResponse> { response ->
            logger.info("Engine responded with previz name ${response.value}")
            onUpdate.doOnce {
                previzManager.startPreviz(response.value)
                window.title = "PaperVision - ${response.value}"
            }
        })
    }

    fun clearToasts() {
        windows.inmutable.filterIsInstance<ToastWindow>().forEach { it.delete() }
    }
}
