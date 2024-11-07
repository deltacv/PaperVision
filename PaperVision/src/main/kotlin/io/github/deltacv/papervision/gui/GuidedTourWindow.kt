package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.node.vision.featuredet.FindContoursNode
import io.github.deltacv.papervision.node.vision.imageproc.ThresholdNode
import io.github.deltacv.papervision.node.vision.overlay.DrawContoursNode
import io.github.deltacv.papervision.util.flags

typealias Stage = GuidedTourWindow.() -> Unit

val Next: GuidedTourWindow.() -> Boolean = {
    ImGui.button("Next")
}

val Close: Stage = {
    if (ImGui.button("Close")) {
        delete()
    }
}

val InitialStage: Stage = {
    centerWindow()

    ImGui.text("Welcome to the guided tour!")
    ImGui.text("This will help you get started with PaperVision.")

    if (Next()) {
        currentStage = Stage1
    }

    ImGui.sameLine()
    Close()
}

val Stage1: Stage = {
    position = ImVec2(
        nodeEditor.inputNode.position.x,
        nodeEditor.inputNode.position.y + (nodeEditor.inputNode.size.y / 2f)
    )

    ImGui.text("A pipeline is a sequence of nodes that transform data in a series of steps.")
    ImGui.text("This is the entry point, from which the pipeline receives the input images.")

    if (Next()) {
        currentStage = Stage2
    }

    ImGui.sameLine()
    Close()
}

val Stage2: Stage = {
    position = ImVec2(
        nodeEditor.outputNode.position.x - size.x + nodeEditor.outputNode.size.x,
        nodeEditor.outputNode.position.y + (nodeEditor.outputNode.size.y * 0.8f)
    )

    ImGui.text("This is the exit point, where the pipeline outputs the final result.")
    ImGui.text("The output image can be visualized, you can draw various graphics and")
    ImGui.text("annotations that represent the results of your processing.")


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

    ImGui.text("Press SPACE or")
    ImGui.text("click this button \\/")

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
            ImGui.text("You need to add the \"Color Threshold\" node to proceed.")
            ImGui.text("Try opening the node list and dragging the node to the editor.")
            Close()
        }
    } else {
        ImGui.text("This is the node list. Here you can find all the nodes available to you.")
        ImGui.text("You can drag and drop nodes from here to the editor to create a pipeline.")
        ImGui.text("")
        ImGui.text("**Grab the \"Color Threshold\" Node to proceed.**")

        Close()

        nodeEditor.paperVision.nodeList.highlight(ThresholdNode::class.java)
    }
}

val Stage5: Stage = {
    focus = false
    val node = nodeEditor.paperVision.nodes.find { it is ThresholdNode } as ThresholdNode?

    if (node == null) {
        ImGui.text("You need to add the \"Color Threshold\" node to proceed.")
        Close()
    } else {
        centerWindow()

        ImGui.text("Great! You've added the \"Color Threshold\" node.")
        ImGui.text("Now, let's connect the input node to the threshold node.")
        ImGui.text("")
        ImGui.text("Click on the pin of the input attribute of this node.")
        ImGui.text("Then, drag the connection to the input node of the pipeline.")

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
        ImGui.text("You need to add the \"Color Threshold\" node to proceed.")
        Close()
    } else {
        position = ImVec2(
            node.input.position.x + node.size.x,
            node.input.position.y
        )

        ImGui.text("Great! Let's add a few more nodes to the pipeline.")
        ImGui.text("Open the node list and drag the \"Simple Find Contours\" node to the editor.")

        Close()

        if (nodeEditor.paperVision.nodeList.isNodesListOpen) {
            currentStage = Stage7
        }
    }
}

val Stage7: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is FindContoursNode } as FindContoursNode?

    if (node == null) {
        ImGui.text("You need to add the \"Simple Find Contours\" node to proceed.")
        Close()

        nodeEditor.paperVision.nodeList.highlight(FindContoursNode::class.java)
    } else {
        centerWindow()

        ImGui.text("You've added the \"Simple Find Contours\" node.")
        ImGui.text("Now, connect the threshold node to it !")

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

        ImGui.text("Great! You've connected the nodes. Let's add one more node !")
        ImGui.text("Open the node list and drag the \"Draw Contours\" node to the editor.")

        Close()

        if (nodeEditor.paperVision.nodeList.isNodesListOpen) {
            currentStage = Stage9
        }
    }
}

val Stage9: Stage = {
    val node = nodeEditor.paperVision.nodes.find { it is DrawContoursNode } as DrawContoursNode?

    if (node == null) {
        ImGui.text("You need to add the \"Draw Contours\" node to proceed.")
        Close()

        nodeEditor.paperVision.nodeList.highlight(DrawContoursNode::class.java)
    } else {
        centerWindow()

        ImGui.text("You've added the \"Draw Contours\" node.")
        ImGui.text("Now, connect the \"Simple Find Contours\" node to it !")
        ImGui.text("Ensure you are connecting it to the right input type.")

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

        ImGui.text("Almost there ! Link the \"Input\" attribute of this node")
        ImGui.text("to the \"Pipeline Input\", all the way to the beginning !.")

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

        ImGui.text("Connect the output of the \"Draw Contours\" node to the")
        ImGui.text("\"Pipeline Output\" to wrap up the process.")

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

    ImGui.text("You're all set !")
    ImGui.text("Press this button to run the pipeline. \\/")

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

        ImGui.text("Click on the Eye button to visualize the output of certain nodes.")
        ImGui.text("This lets you see the output at different steps of the pipeline.")
        ImGui.text("Play around with the node's threshold values to see how it affects the output.")

        if(Next() || node.output.isPrevizEnabled) {
            currentStage = Stage14
        }

        ImGui.sameLine()
        Close()
    }
}

val Stage14: Stage = {
    val floatingButton = nodeEditor.sourceCodeExportButton

    position = ImVec2(
        floatingButton.position.x - size.x + floatingButton.size.x,
        floatingButton.position.y - size.y
    )

    ImGui.text("That's it for the guided tour. Have fun !")
    ImGui.text("After you're done, you can export the pipeline to different formats.")
    ImGui.text("Press this button to export your pipeline as source code. \\/")

    if(floatingButton.isPressed) {
        delete()
    }
}

class GuidedTourWindow(
    val font: Font,
    val nodeEditor: NodeEditor
) : Window() {

    override var title = "Guided Tour"

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
            ImGui.text("This guided tour is not available for existing projects.")
            ImGui.text("Please create a new project and try again.")

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