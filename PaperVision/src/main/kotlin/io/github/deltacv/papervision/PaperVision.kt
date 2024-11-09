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
import imgui.flag.ImGuiKey
import io.github.deltacv.mai18n.Language
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
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplay
import io.github.deltacv.papervision.gui.style.CurrentStyles
import io.github.deltacv.papervision.gui.style.imnodes.ImNodesDarkStyle
import io.github.deltacv.papervision.gui.util.Popup
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.id.IdElementContainer
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.id.NoneIdElement
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.NodeRegistry
import io.github.deltacv.papervision.platform.*
import io.github.deltacv.papervision.serialization.PaperVisionSerializer
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis

class PaperVision(
    private val setupCall: PlatformSetupCallback
) {

    companion object {
        var imnodesStyle
            set(value) { CurrentStyles.imnodesStyle = value }
            get() = CurrentStyles.imnodesStyle

        lateinit var defaultImGuiFont: Font
            private set
        lateinit var defaultImGuiFontSmall: Font
            private set

        val miscIds = IdElementContainer<NoneIdElement>()

        init {
            imnodesStyle = ImNodesDarkStyle
        }
    }

    val logger by loggerForThis()

    lateinit var setup: PlatformSetup
        private set
    lateinit var window: PlatformWindow
        private set
    lateinit var textureFactory: PlatformTextureFactory
        private set
    lateinit var config: PlatformConfig
        private set

    val onInit = PaperVisionEventHandler("PaperVision-OnInit")
    val onUpdate = PaperVisionEventHandler("PaperVision-OnUpdate")
    val onDeserialization = PaperVisionEventHandler("PaperVision-OnDeserialization")

    lateinit var textureProcessorQueue: TextureProcessorQueue
        private set

    lateinit var keyManager: KeyManager
        private set

    val codeGenManager = CodeGenManager(this)
    val fontManager = FontManager()

    val langManager = Language("/lang.csv", "en").makeTr()

    val nodeEditor by lazy { NodeEditor(this, keyManager) }
    val nodeList by lazy { NodeList(this, keyManager, NodeRegistry.nodes) }

    val nodes = IdElementContainer<Node<*>>()
    val attributes = IdElementContainer<Attribute>()
    val links = IdElementContainer<Link>()
    val windows = IdElementContainer<Window>()
    val textures = IdElementContainer<PlatformTexture>()
    val streamDisplays = IdElementContainer<ImageDisplay>()
    val actions = IdElementContainer<Action>()

    val popups = IdElementContainer<Popup>()

    val isModalWindowOpen get() = windows.inmutable.find { it.isModal && it.isVisible } != null

    val isPopupOpen get() = popups.inmutable.find { it.isVisible } != null

    lateinit var engineClient: PaperVisionEngineClient

    lateinit var previzManager: ClientPrevizManager
        private set

    lateinit var defaultFont: Font
        private set

    lateinit var defaultFontBig: Font
        private set

    lateinit var codeFont: Font
        private set

    lateinit var fontAwesome: Font
        private set
    lateinit var fontAwesomeBig: Font
        private set

    lateinit var fontAwesomeBrands: Font
        private set
    lateinit var fontAwesomeBrandsBig: Font
        private set

    fun init() {
        IdElementContainerStack.threadStack.push(nodes)
        IdElementContainerStack.threadStack.push(attributes)
        IdElementContainerStack.threadStack.push(links)
        IdElementContainerStack.threadStack.push(windows)
        IdElementContainerStack.threadStack.push(textures)
        IdElementContainerStack.threadStack.push(streamDisplays)
        IdElementContainerStack.threadStack.push(actions)
        IdElementContainerStack.threadStack.push(popups)

        logger.info("Starting PaperVision...\n\n${IntroModalWindow.iconLogo}\n")

        logger.info("Using the ${setupCall.name} platform")
        setup = setupCall.setup()

        keyManager = KeyManager(setup.keys ?: throw IllegalArgumentException("Platform ${setup.name} must provide PlatformKeys"))
        window = setup.window ?: throw IllegalArgumentException("Platform ${setup.name} must provide a Window")
        textureFactory = setup.textureFactory ?: throw IllegalArgumentException("Platform ${setup.name} must provide a TextureFactory")
        config = setup.config

        config.load()

        if(!langManager.availableLangs.contains(config.fields.lang)) {
            langManager.lang = config.fields.lang
        } else {
            logger.warn("Configured language ${config.fields.lang} is not available, defaulting")
        }

        textureProcessorQueue = TextureProcessorQueue(textureFactory)
        textureProcessorQueue.subscribeTo(onUpdate)

        engineClient = PaperVisionEngineClient(setup.engineBridge ?: NoOpPaperVisionEngineBridge)
        previzManager = ClientPrevizManager(160, 120, codeGenManager, textureProcessorQueue, engineClient, setup.previzByteMessageReceiverProvider)

        engineClient.connect()

        // disable annoying ini file creation (hopefully shouldn't break anything)
        ImGui.getIO().iniFilename = null
        ImGui.getIO().logFilename = null

        // initializing fonts right after the imgui context is created
        // we can't create fonts mid-frame so that's kind of a problem
        defaultFont = fontManager.makeFont("/fonts/Calcutta-SemiBold.otf", defaultFontConfig(20f))
        defaultFontBig = fontManager.makeFont("/fonts/Calcutta-SemiBold.otf", defaultFontConfig(28f))

        codeFont = fontManager.makeFont("/fonts/JetBrainsMono-Regular.ttf", defaultFontConfig(28f))
        defaultImGuiFont = fontManager.makeDefaultFont(20f)
        defaultImGuiFontSmall = fontManager.makeDefaultFont(12f)

        val rangesBuilder = ImFontGlyphRangesBuilder()
        rangesBuilder.addRanges(ImGui.getIO().fonts.glyphRangesDefault)
        rangesBuilder.addRanges(FontAwesomeIcons._IconRange)

        fontAwesome = fontManager.makeFont("/fonts/icons/FontAwesome6-Free-Solid-900.otf", defaultFontConfig(16f), rangesBuilder.buildRanges())
        fontAwesomeBig = fontManager.makeFont("/fonts/icons/FontAwesome6-Free-Solid-900.otf", defaultFontConfig(52f), rangesBuilder.buildRanges())

        fontAwesomeBrands = fontManager.makeFont("/fonts/icons/FontAwesome6-Brands-Regular-400.otf", defaultFontConfig(16f), rangesBuilder.buildRanges())
        fontAwesomeBrandsBig = fontManager.makeFont("/fonts/icons/FontAwesome6-Brands-Regular-400.otf", defaultFontConfig(80f), rangesBuilder.buildRanges())

        nodeEditor.enable()
        langManager.loadIfNeeded()

        nodeList.enable()

        RootAction().enable()

        onInit.run()

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()
        IdElementContainerStack.threadStack.pop<Link>()
        IdElementContainerStack.threadStack.pop<Window>()
        IdElementContainerStack.threadStack.pop<PlatformTexture>()
        IdElementContainerStack.threadStack.pop<ImageDisplay>()
        IdElementContainerStack.threadStack.pop<Action>()
        IdElementContainerStack.threadStack.pop<Popup>()

        logger.info("PaperVision started")
    }

    fun firstProcess() {
        window.title = "PaperVision"
        window.icon = "/ico/ico_ezv.png"
        window.maximized = true

        onUpdate.doOnce {
            if(setup.showWelcomeWindow) {
                IntroModalWindow(
                    defaultImGuiFontSmall,
                    codeFont,
                    defaultFontBig,
                    nodeEditor
                ).enable()
            } else {
                onDeserialization {
                    logger.info("showWelcome = ${nodeEditor.flags["showWelcome"]}")

                    if (nodeEditor.flags.getOrElse("showWelcome", { true })) {
                        IntroModalWindow(
                            defaultImGuiFontSmall,
                            codeFont,
                            defaultFontBig,
                            nodeEditor
                        ).apply {
                            onDontShowAgain {
                                nodeEditor.flags["showWelcome"] = false
                                logger.info("showWelcome = ${nodeEditor.flags["showWelcome"]}")
                            }
                        }.enable()
                    }
                }
            }
        }
    }

    fun process() {
        IdElementContainerStack.threadStack.push(nodes)
        IdElementContainerStack.threadStack.push(attributes)
        IdElementContainerStack.threadStack.push(links)
        IdElementContainerStack.threadStack.push(windows)
        IdElementContainerStack.threadStack.push(textures)
        IdElementContainerStack.threadStack.push(streamDisplays)
        IdElementContainerStack.threadStack.push(actions)
        IdElementContainerStack.threadStack.push(popups)

        if(keyManager.pressing(keyManager.keys.LeftControl)) {
            if(ImGui.isKeyPressed(ImGuiKey.Z)) {
                undo()
            } else if(ImGui.isKeyPressed(ImGuiKey.Y)) {
                redo()
            } else if(ImGui.isKeyPressed(ImGuiKey.S)) {
                logger.info(PaperVisionSerializer.serialize(nodes.inmutable, links.inmutable))
            }
        }

        onUpdate.run()

        engineClient.process()

        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = window.size
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        for(window in windows.inmutable) {
            window.draw()
        }
        for(popup in popups.inmutable) {
            popup.draw()
        }

        ImGui.popFont()

        keyManager.update()

        previzManager.update()

        IdElementContainerStack.threadStack.pop<Node<*>>()
        IdElementContainerStack.threadStack.pop<Attribute>()
        IdElementContainerStack.threadStack.pop<Link>()
        IdElementContainerStack.threadStack.pop<Window>()
        IdElementContainerStack.threadStack.pop<PlatformTexture>()
        IdElementContainerStack.threadStack.pop<ImageDisplay>()
        IdElementContainerStack.threadStack.pop<Action>()
        IdElementContainerStack.threadStack.pop<Popup>()
    }

    fun undo() {
        logger.info("undo | stack; size: ${actions.size}, pointer: ${actions.stackPointer}, peek: ${actions.peek()}")
        actions.peekAndPushback()?.undo()
    }

    fun redo() {
        logger.info("redo | stack; size: ${actions.size}, pointer: ${actions.stackPointer}, peek: ${actions.peek()}")

        actions.pushforwardIfNonNull()
        actions.peek()?.execute()
    }

    fun destroy() {
        nodeEditor.destroy()
    }

    fun startPrevizAsk() {
        engineClient.sendMessage(PrevizAskNameMessage().onResponseWith<StringResponse> { response ->
            logger.info("Engine responded with previz name ${response.value}")
            onUpdate.doOnce {
                previzManager.startPreviz(response.value)
                window.title = "PaperVision - ${response.value}"
            }
        })
    }
}