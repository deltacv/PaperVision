package io.github.deltacv.papervision.node.vision

import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.PaperNode
import java.lang.IndexOutOfBoundsException

@PaperNode(
    name = "nod_pipelineinput",
    category = Category.FLOW,
    showInList = false
)
class InputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    override fun init() {
        editor.onDraw.doOnce {
            if(serializedId != null) return@doOnce
            windowSizeSupplier?.let {
                val nodeSize = ImVec2()
                ImNodes.getNodeDimensions(id, nodeSize)

                val windowSize = it()
                ImNodes.setNodeScreenSpacePos(id, nodeSize.x * 0.5f, windowSize.y / 2f - nodeSize.y / 2)
            }
        }
    }

    val output = MatAttribute(OUTPUT, "$[att_input]")

    override fun onEnable() {
        + output.rebuildOnChange()
    }

    fun ensureAttributeExists() { // prevent weird oopsies due to the special way these persistent buddies are handled
        enable()
        output.enable()
    }

    override fun genCode(current: CodeGen.Current) = NoSession

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    val value = GenValue.Mat(Variable(OpenCvTypes.Mat, "input"), ColorSpace.RGBA)

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute) = value
}

@PaperNode(
    name = "nod_pipelineoutput",
    category = Category.FLOW,
    showInList = false
)
class OutputMatNode @JvmOverloads constructor(
    var windowSizeSupplier: (() -> ImVec2)? = null
) : DrawNode<NoSession>(allowDelete = false) {

    var streamId: Int? = null

    init {
        genOptions {
            genAtTheEnd = true
        }
    }

    override fun init() {
        editor.onDraw.doOnce {
            if(serializedId != null) return@doOnce

            windowSizeSupplier?.let {
                val nodeSize = ImVec2()
                ImNodes.getNodeDimensions(id, nodeSize)

                val windowSize = it()
                ImNodes.setNodeScreenSpacePos(id, windowSize.x - nodeSize.x * 1.5f , windowSize.y / 2f - nodeSize.y / 2)
            }
        }
    }

    val input = MatAttribute(INPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
    }

    fun ensureAttributeExists() { // prevent weird oopsies due to the special way these persistent buddies are handled
        enable()
        input.enable()
    }

    override fun genCode(current: CodeGen.Current) = current {
        current.scope {
            streamMat(streamId!!, input.value(current).value, input.value(current).color)

            returnMethod(input.value(current).value)
            appendWhiteline = false
        }

        NoSession
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}