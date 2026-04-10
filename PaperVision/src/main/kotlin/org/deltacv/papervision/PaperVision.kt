/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.deltacv.papervision

import imgui.ImFontGlyphRangesBuilder
import imgui.ImGui
import imgui.flag.ImGuiCond
import org.deltacv.papervision.action.Action
import org.deltacv.papervision.action.RootAction
import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.codegen.CodeGenManager
import org.deltacv.papervision.engine.bridge.NoOpPaperVisionEngineBridge
import org.deltacv.papervision.engine.client.PaperVisionEngineClient
import org.deltacv.papervision.engine.client.message.PrevizAskNameMessage
import org.deltacv.papervision.engine.client.response.StringResponse
import org.deltacv.papervision.engine.previz.ClientPrevizManager
import org.deltacv.papervision.gui.*
import org.deltacv.papervision.gui.display.ImageDisplay
import org.deltacv.papervision.gui.editor.menu.IntroModalWindow
import org.deltacv.papervision.gui.editor.NodeEditor
import org.deltacv.papervision.gui.style.CurrentStyles
import org.deltacv.papervision.gui.style.imnodes.ImNodesDarkStyle
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.gui.font.FontManager
import org.deltacv.papervision.gui.Popup
import org.deltacv.papervision.gui.Window
import org.deltacv.papervision.gui.font.defaultFontConfig
import org.deltacv.papervision.id.*
import org.deltacv.papervision.id.container.IdContainer
import org.deltacv.papervision.id.container.IdContainerStack
import org.deltacv.papervision.id.container.SingleIdContainer
import org.deltacv.papervision.id.container.DenseIdContainer
import org.deltacv.papervision.id.container.SparseIdContainer
import org.deltacv.papervision.id.container.StackIdContainer
import org.deltacv.papervision.io.KeyManager
import org.deltacv.papervision.io.TextureProcessorQueue
import org.deltacv.papervision.node.Link
import org.deltacv.papervision.node.Node
import org.deltacv.papervision.platform.*
import org.deltacv.papervision.serialization.v2.PaperVisionProject
import org.deltacv.papervision.serialization.v2.json.JsonCodec
import org.deltacv.papervision.util.event.PaperEventHandler
import org.deltacv.papervision.util.loggerForThis
import org.deltacv.mai18n.Language
import org.deltacv.mai18n.makeThreadTr
import java.awt.Taskbar
import java.awt.Toolkit

class PaperVision(
    private val platformSetupCallback: PlatformSetupCallback
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

    val onInit            = PaperEventHandler("PaperVision-OnInit")
    val onUpdate          = PaperEventHandler("PaperVision-OnUpdate")
    val onDeserialization = PaperEventHandler("PaperVision-OnDeserialization")

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

    val nodes: IdContainer<Node<*>>                                = DenseIdContainer()
    val attributes: IdContainer<Attribute>                         = DenseIdContainer()
    val links: IdContainer<Link>                                   = DenseIdContainer()
    val windows: IdContainer<Window>                               = DenseIdContainer()
    val textures: IdContainer<PlatformTexture>                     = DenseIdContainer()
    val textureProcessorQueues: IdContainer<TextureProcessorQueue> = SingleIdContainer()
    val fonts: IdContainer<Font>                                   = SparseIdContainer()
    val streamDisplays: IdContainer<ImageDisplay>                  = DenseIdContainer()
    val actions: StackIdContainer<Action>                          = StackIdContainer()
    val popups: IdContainer<Popup>                                 = DenseIdContainer()
    val misc: IdContainer<Misc>                                    = SparseIdContainer()

    lateinit var engineClient: PaperVisionEngineClient
    lateinit var previzManager: ClientPrevizManager

    lateinit var defaultFont: Font

    fun init() = withIdContainers {
        logger.info("Starting PaperVision...\n\n${IntroModalWindow.iconLogo}\n")
        logger.info("Using the ${platformSetupCallback.name} platform")

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
        setup = platformSetupCallback.setup()
        keyManager = KeyManager(setup.keys ?: error("Platform ${setup.name} must provide PlatformKeys"))
        window = setup.window ?: error("Platform ${setup.name} must provide a Window")
        textureFactory = setup.textureFactory ?: error("Platform ${setup.name} must provide a TextureFactory")
        config = setup.config
        config.load()

        // serialize and log
        keyManager.addShortcut(keyManager.keys.NativeLeftSuper, keyManager.keys.Spacebar) {
            logger.info("-- Serialized project for Ctrl + Spacebar debugging --")
            logger.info(JsonCodec().encode(PaperVisionProject.from(this)))
        }

        // close the topmost modal window on escape
        keyManager.addShortcut(keyManager.keys.Escape) {
            for(window in windows.inmutable) {
                if(window.isEnabled && window.isModal) {
                    window.delete()
                    break
                }
            }
        }
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

        font("jetbrains-mono", "/fonts/JetBrainsMono-Regular.ttf", 20f, ranges)
        font("jetbrains-mono-big", "/fonts/JetBrainsMono-Regular.ttf", 28f)

        fontManager.makeDefaultFont(20) // "default-20"
        fontManager.makeDefaultFont(12) // "default-12"

        // Pass the ranges to the icon fonts so FontAwesome glyphs are available
        font("font-awesome", "/fonts/icons/FontAwesome6-Free-Solid-900.otf", 16f, ranges)
        font("font-awesome-big", "/fonts/icons/FontAwesome6-Free-Solid-900.otf", 52f, ranges)
        font("font-awesome-brands", "/fonts/icons/FontAwesome6-Brands-Regular-400.otf", 16f, ranges)
        font("font-awesome-brands-big", "/fonts/icons/FontAwesome6-Brands-Regular-400.otf", 80f, ranges)
    }

    private fun initUI() {
        nodeEditor.enable()
    }

    private fun dumpStartupDiagnostics() {
        logger.debug("--  PaperVision JVM startup diagnostics --")

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

        onUpdate.once {
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

    fun process() = withIdContainers {
        onUpdate.run()
        engineClient.process()

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)
        val size = window.size
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        windows.forEach { it.draw() }
        popups.forEach { it.draw() }

        textureProcessorQueues.forEach { it.draw() }

        ImGui.popFont()

        keyManager.update()
        previzManager.update()
    }

    fun destroy() {
        logger.info("Shutting down PaperVision...")

        config.save()

        engineClient.disconnect()

        textureProcessorQueue.delete()

        windows.reversed().forEach { it.delete() }
        popups.reversed().forEach { it.delete() }

        nodeEditor.delete()
    }

    fun changeLanguage(langCode: String) {
        currentLanguage = Language("/lang_pv.csv", langCode).apply { makeThreadTr() }
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

    fun startPrevizWithEngine() {
        engineClient.sendMessage(PrevizAskNameMessage().onResponseWith<StringResponse> { response ->
            logger.info("Engine responded with previz name ${response.value}")
            onUpdate.once {
                previzManager.startPreviz(response.value)
                window.title = "PaperVision - ${response.value}"
            }
        })
    }

    fun clearToasts() {
        windows.filterIsInstance<ToastWindow>().forEach { it.delete() }
    }

    /** Helper to simplify font creation */
    private fun font(name: String, path: String, size: Float, ranges: ShortArray? = null) =
        fontManager.makeFont(name, path, defaultFontConfig(size), ranges)

    /** Executes a block of code with all containers pushed/popped safely */
    private inline fun withIdContainers(crossinline block: () -> Unit) {
        IdContainerStack.local.push(nodes)
        IdContainerStack.local.push(attributes)
        IdContainerStack.local.push(links)
        IdContainerStack.local.push(windows)
        IdContainerStack.local.push(textures)
        IdContainerStack.local.push(textureProcessorQueues)
        IdContainerStack.local.push(fonts)
        IdContainerStack.local.push(streamDisplays)
        IdContainerStack.local.push(actions)
        IdContainerStack.local.push(popups)
        IdContainerStack.local.push(misc)

        try {
            block()
        } finally {
            IdContainerStack.local.pop<Node<*>>()
            IdContainerStack.local.pop<Attribute>()
            IdContainerStack.local.pop<Link>()
            IdContainerStack.local.pop<Window>()
            IdContainerStack.local.pop<PlatformTexture>()
            IdContainerStack.local.pop<TextureProcessorQueue>()
            IdContainerStack.local.pop<Font>()
            IdContainerStack.local.pop<ImageDisplay>()
            IdContainerStack.local.pop<Action>()
            IdContainerStack.local.pop<Popup>()
            IdContainerStack.local.pop<Misc>()
        }
    }
}



