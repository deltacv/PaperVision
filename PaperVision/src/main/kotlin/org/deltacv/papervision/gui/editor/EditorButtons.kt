package org.deltacv.papervision.gui.editor

import imgui.ImVec2
import org.deltacv.papervision.PaperVision
import org.deltacv.papervision.engine.bridge.NoOpPaperVisionEngineBridge
import org.deltacv.papervision.gui.ButtonWindow
import org.deltacv.papervision.gui.ToastWindow
import org.deltacv.papervision.gui.editor.menu.OptionsWindow
import org.deltacv.papervision.gui.editor.menu.SourceCodeLanguageWindow
import org.deltacv.papervision.gui.style.hexColor
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.font.FontAwesomeIcons

class NodeListButton(
    val nodeList: NodeList,
) : ButtonWindow(
    buttonText = FontAwesomeIcons.Plus,
    buttonFont = Font.find("font-awesome-big"),
    buttonTooltip = "mis_nodeslist_open",
    buttonTooltipFont = Font.find("calcutta-big")
) {
    init {
        onClick {
            if(!nodeList.isNodesListOpen) {
                nodeList.showList()
            } else {
                nodeList.closeList()
            }
        }
    }

    override fun preDrawContents() {
        buttonText = if (nodeList.isNodesListOpen) "X" else FontAwesomeIcons.Plus
        buttonTooltip = if (nodeList.isNodesListOpen) "mis_nodeslist_close" else "mis_nodeslist_open"

        super.preDrawContents()
    }
}

class OptionsButtonWindow(private val options: Map<String, Option>) : ButtonWindow(
    FontAwesomeIcons.Gear,
    Font.find("font-awesome-big")
) {
    init {
        onClick {
            OptionsWindow(options).enable()
        }
    }
}

class SourceCodeExportButtonWindow(
    private val nodeEditorSizeSupplier: () -> ImVec2,
    private val paperVision: PaperVision
) : ButtonWindow(
    FontAwesomeIcons.Code,
    Font.find("font-awesome-big")
) {
    init {
        onClick {
            SourceCodeLanguageWindow(paperVision, nodeEditorSizeSupplier).enable()
        }
    }
}

class PlayButtonWindow(
    private val paperVision: PaperVision,
) : ButtonWindow(
    FontAwesomeIcons.Play,
    Font.find("font-awesome-big")
) {
    init {
        onClick {
            if(paperVision.engineClient.bridge is NoOpPaperVisionEngineBridge) {
                ToastWindow("err_noop_engine", font = Font.find("calcutta-big")).enable()
            } else {
                if (!paperVision.previzManager.previzRunning) {
                    paperVision.startPrevizWithEngine()
                } else {
                    paperVision.previzManager.stopPreviz()
                }
            }
        }
    }

    override var buttonText = FontAwesomeIcons.Play
        get() =
            if (paperVision.previzManager.previzRunning) {
                FontAwesomeIcons.Stop
            } else FontAwesomeIcons.Play

    override val colors get() = if(paperVision.previzManager.previzRunning) {
        Colors(
            base = hexColor("#6B3A3A", 0.8f),
            hover = hexColor("#8A4D4D"),
            active = hexColor("#552E2E")
        )
    } else {
        Colors(
            base = hexColor("#3A5F4B", 0.8f),
            hover = hexColor("#4C7A60"),
            active = hexColor("#2F4F3E")
        )
    }
}
