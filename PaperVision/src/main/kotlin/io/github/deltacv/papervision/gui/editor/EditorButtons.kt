package io.github.deltacv.papervision.gui.editor

import imgui.ImVec2
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.engine.bridge.NoOpPaperVisionEngineBridge
import io.github.deltacv.papervision.gui.ButtonWindow
import io.github.deltacv.papervision.gui.ToastWindow
import io.github.deltacv.papervision.gui.editor.menu.OptionsWindow
import io.github.deltacv.papervision.gui.editor.menu.SourceCodeExportSelectLanguageWindow
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons

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
            SourceCodeExportSelectLanguageWindow(paperVision, nodeEditorSizeSupplier).enable()
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

    override fun preDrawContents() {
        buttonText = if (paperVision.previzManager.previzRunning) {
            FontAwesomeIcons.Stop
        } else FontAwesomeIcons.Play

        super.preDrawContents()
    }
}