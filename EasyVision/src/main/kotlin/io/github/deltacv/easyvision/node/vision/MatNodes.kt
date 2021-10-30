package io.github.deltacv.easyvision.node.vision

import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.RegisterNode
import java.lang.IllegalArgumentException

@RegisterNode(
    name = "nod_pipelineinput",
    category = Category.FLOW,
    showInList = false
)
class InputMatNode @JvmOverloads constructor(
    val windowSizeSupplier: () -> ImVec2 = {
        throw IllegalArgumentException("A window size is needed")
    }
) : DrawNode<NoSession>(allowDelete = false) {

    override fun init() {
        val nodeSize = ImVec2()
        ImNodes.getNodeDimensions(id, nodeSize)

        val windowSize = windowSizeSupplier()
        ImNodes.setNodeScreenSpacePos(id, nodeSize.x * 0.5f, windowSize.y / 2f - nodeSize.y / 2)
    }

    override fun onEnable() {
        + MatAttribute(OUTPUT, "$[att_input]")
    }

    override fun genCode(current: CodeGen.Current) = NoSession

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    val value = GenValue.Mat("input".v, Colors.RGBA)

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute) = value
}

@RegisterNode(
    name = "nod_pipelineoutput",
    category = Category.FLOW,
    showInList = false
)
class OutputMatNode @JvmOverloads constructor(
    val windowSizeSupplier: () -> ImVec2 = {
        throw IllegalArgumentException("A window size is needed")
    }
) : DrawNode<NoSession>(allowDelete = false) {

    override fun init() {
        val nodeSize = ImVec2()
        ImNodes.getNodeDimensions(id, nodeSize)

        val windowSize = windowSizeSupplier()
        ImNodes.setNodeScreenSpacePos(id, windowSize.x - nodeSize.x * 1.5f , windowSize.y / 2f - nodeSize.y / 2)
    }

    val input = MatAttribute(INPUT, "$[att_output]")

    override fun onEnable() {
        + input
    }

    override fun genCode(current: CodeGen.Current) = current {
        current.scope {
            returnMethod(input.value(current).value) // start code gen!
            appendWhiteline = false
        }

        NoSession
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}