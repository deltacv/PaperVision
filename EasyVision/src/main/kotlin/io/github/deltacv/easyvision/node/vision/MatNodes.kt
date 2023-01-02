package io.github.deltacv.easyvision.node.vision

import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.build.Variable
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "nod_pipelineinput",
    category = Category.FLOW,
    showInList = false
)
class InputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    override fun init() {
        windowSizeSupplier?.let {
            val nodeSize = ImVec2()
            ImNodes.getNodeDimensions(id, nodeSize)

            val windowSize = it()
            ImNodes.setNodeScreenSpacePos(id, nodeSize.x * 0.5f, windowSize.y / 2f - nodeSize.y / 2)
        }
    }

    val output = MatAttribute(OUTPUT, "$[att_input]")

    override fun onEnable() {
        + output.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = NoSession

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    val value = GenValue.Mat(Variable(OpenCvTypes.Mat, "input"), ColorSpace.RGBA)

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute) = value
}

@RegisterNode(
    name = "nod_pipelineoutput",
    category = Category.FLOW,
    showInList = false
)
class OutputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    init {
        genOptions {
            genAtTheEnd = true
        }
    }

    override fun init() {
        windowSizeSupplier?.let {
            val nodeSize = ImVec2()
            ImNodes.getNodeDimensions(id, nodeSize)

            val windowSize = it()
            ImNodes.setNodeScreenSpacePos(id, windowSize.x - nodeSize.x * 1.5f , windowSize.y / 2f - nodeSize.y / 2)
        }
    }

    val input = MatAttribute(INPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        current.scope {
            returnMethod(input.value(current).value)
            appendWhiteline = false
        }

        NoSession
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}