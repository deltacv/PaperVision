package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.node.vision.featuredet.FindContoursNode
import io.github.deltacv.papervision.node.vision.imageproc.ThresholdNode
import io.github.deltacv.papervision.node.vision.overlay.DrawContoursNode
import io.github.deltacv.papervision.util.flags

typealias Stage = GuidedTourWindow.() -> Unit

val Next: GuidedTourWindow.() -> Boolean = {
    ImGui.button(tr("mis_next"))
}

val Close: Stage = {
    if (ImGui.button(tr("mis_cancel"))) {
        delete()
    }
}

val InitialStage: Stage = {
    centerWindow()

    ImGui.text(tr("mis_guidedtour_1"))
    ImGui.text(tr("mis_guidedtour_2"))

    if (Next()) {
        currentStage = Stage1
    }

    ImGui.sameLine()
    Close()
}

val Stage1: Stage = {
    position = ImVec2(
        nodeEditor.inputNode.screenPosition.x,
        nodeEditor.inputNode.screenPosition.y + (nodeEditor.inputNode.size.y / 2f)
    )

    ImGui.text(tr("mis_guidedtour_3"))
    ImGui.text(tr("mis_guidedtour_4"))

    if (Next()) {
        currentStage = Stage2
    }

    ImGui.sameLine()
    Close()
}

val Stage2: Stage = {
    position = ImVec2(
        nodeEditor.outputNode.screenPosition.x - size.x + nodeEditor.outputNode.size.x,
        nodeEditor.outputNode.screenPosition.y + (nodeEditor.outputNode.size.y * 0.8f)
    )

    ImGui.text(tr("mis_guidedtour_5"))
    ImGui.text(tr("mis_guidedtour_6"))


    if (Next()) {
        currentStage = Stage3
    }

    ImGui.sameLine()
    Close()
}

val Stage3: Stage = {
    val floatingButton = nodeEditor.paperVision.nodeList.floatingButton

    position = ImVec2(
        floatingButton.position.x - size.x + floatingButton.size.x,
        floatingButton.position.y - size.y
    )

    ImGui.text(tr("mis_guidedtour_7"))

    if (nodeEditor.paperVision.nodeList.isNodesListOpen) {
        currentStage = Stage4
    }
}

val Stage4: Stage = {
    // bottom center
    position = ImVec2(
       (nodeEditor.paperVision.nodeList.size.x / 2) - (size.x / 2),
        nodeEditor.paperVision.nodeList.size.y - size.y - 50
    )

    focus = true

    if (!nodeEditor.paperVision.nodeList.isNodesListOpen) {
        if (nodeEditor.paperVision.nodes.find { it is ThresholdNode } != null) {
            currentStage = Stage5
        } else {
            ImGui.text(tr("mis_guidedtour_9"))
            Close()
        }
    } else {
        ImGui.text(tr("mis_guidedtour_8_0"))
        ImGui.text(tr("mis_guidedtour_8_1"))
        ImGui.text("")
        ImGui.text(tr("mis_guidedtour_8_2"))

        Close()

        nodeEditor.paperVision.nodeList.highlight(ThresholdNode::class.java)
    }
}

val Stage5: Stage = {
    focus = false
    val node = nodeEditor.paperVision.nodes.find { it is ThresholdNode } as ThresholdNode?

    if (node == null) {
        ImGui.text(tr("mis_guidedtour_9"))
        Close()
    } else {
        centerWindow()

        ImGui.text(tr("mis_guidedtour_10"))
        ImGui.text(tr("mis_guidedtour_11_0"))
        ImGui.text("")
        ImGui.text(tr("mis_guidedtour_11_1"))
        ImGui.text(tr("mis_guidedtour_11_2"))

        Close()

        position = ImVec2(
            node.input.position.x + node.size.x,
            node.input.position.y
        )

        if (node.input.hasLink) {
            currentStage = Stage6
        }
    }
}

val Stage6: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is ThresholdNode } as ThresholdNode?

    if (node == null) {
        ImGui.text(tr("mis_guidedtour_9"))
        Close()
    } else {
        position = ImVec2(
            node.input.position.x + node.size.x,
            node.input.position.y
        )

        ImGui.text(tr("mis_guidedtour_12"))

        Close()

        if (nodeEditor.paperVision.nodeList.isNodesListOpen) {
            currentStage = Stage7
        }
    }
}

val Stage7: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is FindContoursNode } as FindContoursNode?

    if (node == null) {
        ImGui.text(tr("mis_guidedtour_15"))
        Close()

        nodeEditor.paperVision.nodeList.highlight(FindContoursNode::class.java)
    } else {
        centerWindow()

        ImGui.text(tr("mis_guidedtour_13"))
        ImGui.text(tr("mis_guidedtour_14"))

        Close()

        position = ImVec2(
            node.inputMat.position.x + node.size.x,
            node.inputMat.position.y
        )

        if (node.inputMat.hasLink) {
            currentStage = Stage8
        }
    }
}

val Stage8: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is FindContoursNode } as FindContoursNode?

    if (node == null) {
        currentStage = Stage6
    } else {
        position = ImVec2(
            node.inputMat.position.x + node.size.x,
            node.inputMat.position.y
        )

        ImGui.text(tr("mis_guidedtour_16"))
        ImGui.text(tr("mis_guidedtour_17"))

        Close()

        if (nodeEditor.paperVision.nodeList.isNodesListOpen) {
            currentStage = Stage9
        }
    }
}

val Stage9: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is DrawContoursNode } as DrawContoursNode?

    if (node == null) {
        ImGui.text(tr("mis_guidedtour_18"))
        Close()

        nodeEditor.paperVision.nodeList.highlight(DrawContoursNode::class.java)
    } else {
        centerWindow()

        ImGui.text(tr("mis_guidedtour_19"))
        ImGui.text(tr("mis_guidedtour_20"))
        ImGui.text(tr("mis_guidedtour_21"))

        Close()

        position = ImVec2(
            node.inputMat.position.x + node.size.x,
            node.inputMat.position.y
        )

        if (node.contours.hasLink) {
            currentStage = Stage10
        }
    }
}

val Stage10: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is DrawContoursNode } as DrawContoursNode?

    nodeEditor.paperVision.nodeList.clearHighlight()

    if (node == null) {
        currentStage = Stage6
    } else {
        centerWindow()

        ImGui.text(tr("mis_guidedtour_22"))
        ImGui.text(tr("mis_guidedtour_23"))

        Close()

        position = ImVec2(
            node.inputMat.position.x + node.size.x,
            node.inputMat.position.y
        )

        if (node.inputMat.hasLink) {
            currentStage = Stage11
        }
    }
}

val Stage11: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is DrawContoursNode } as DrawContoursNode?

    if (node == null) {
        currentStage = Stage6
    } else {
        centerWindow()

        position = ImVec2(
            node.inputMat.position.x - size.x,
            node.contours.position.y
        )

        ImGui.text(tr("mis_guidedtour_24"))
        ImGui.text(tr("mis_guidedtour_25"))

        if(node.outputMat.hasLink) {
            currentStage = Stage12
        }

        Close()
    }
}

val Stage12: Stage = {
    val floatingButton = nodeEditor.playButton

    position = ImVec2(
        floatingButton.position.x - size.x + floatingButton.size.x,
        floatingButton.position.y - size.y
    )

    ImGui.text(tr("mis_guidedtour_26"))
    ImGui.text(tr("mis_guidedtour_27"))

    if(floatingButton.isPressed) {
        currentStage = Stage13
    }
}

val Stage13: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is ThresholdNode } as ThresholdNode?

    if(node == null) {
        currentStage = Stage14
    } else {
        position = ImVec2(
            node.output.position.x + node.size.x + 5,
            node.output.position.y
        )

        ImGui.text(tr("mis_guidedtour_28"))
        ImGui.text(tr("mis_guidedtour_29"))
        ImGui.text(tr("mis_guidedtour_30"))

        if(Next() || node.output.isPrevizEnabled) {
            currentStage = Stage14
        }
    }
}

val Stage14: Stage = {
    val floatingButton = nodeEditor.sourceCodeExportButton

    position = ImVec2(
        floatingButton.position.x - size.x + floatingButton.size.x,
        floatingButton.position.y - size.y
    )

    ImGui.text(tr("mis_guidedtour_31"))
    ImGui.text(tr("mis_guidedtour_32"))
    ImGui.text(tr("mis_guidedtour_33"))

    if(floatingButton.isPressed) {
        delete()
    }
}

class GuidedTourWindow(
    val font: Font,
    val nodeEditor: NodeEditor
) : Window() {

    override var title = "$[mis_guidedtour]"

    var currentStage: Stage = InitialStage

    private var previousStage: Stage? = null

    var firstStageDraw: Boolean = true
        private set

    override val windowFlags = flags(
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoResize
    )

    override fun drawContents() {
        if(nodeEditor.paperVision.nodes.elements.size > 4 && currentStage == InitialStage) {
            centerWindow()
            ImGui.text(tr("mis_guidedtour_notavailable_1"))
            ImGui.text(tr("mis_guidedtour_notavailable_2"))

            Close()
            return
        }

        previousStage = currentStage

        ImGui.pushFont(font.imfont)
        currentStage()
        ImGui.popFont()

        firstStageDraw = currentStage != previousStage
    }

    override fun delete() {
        super.delete()
        nodeEditor.paperVision.nodeList.clearHighlight()
    }
}