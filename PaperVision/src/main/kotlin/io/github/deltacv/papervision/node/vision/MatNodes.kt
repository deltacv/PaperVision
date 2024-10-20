package io.github.deltacv.papervision.node.vision

import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.csv
import io.github.deltacv.papervision.codegen.dsl.generatorFor
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

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
                ImNodes.getNodeDimensions(nodeSize, id)

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

    override val generators = mutableMapOf(
        generatorFor(JavaLanguage) { NoSession }
    )

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute
    ) = when(current.language) {
        is CPythonLanguage -> GenValue.Mat(Variable(CPythonLanguage.NoType, "input"), ColorSpace.RGBA)
        else -> GenValue.Mat(Variable(JvmOpenCvTypes.Mat, "input"), ColorSpace.RGBA)
    }
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
                ImNodes.getNodeDimensions(nodeSize, id)

                val windowSize = it()
                ImNodes.setNodeScreenSpacePos(id, windowSize.x - nodeSize.x * 1.5f , windowSize.y / 2f - nodeSize.y / 2)
            }
        }
    }

    val input = MatAttribute(INPUT, "$[att_output]")
    val crosshair = PointsAttribute(INPUT, "$[att_crosshair]")
    val exportedData = ListAttribute(INPUT, DoubleAttribute, "$[att_exporteddata]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + crosshair.rebuildOnChange()
        + exportedData.rebuildOnChange()
    }

    fun ensureAttributeExists() { // prevent weird oopsies due to the special way these persistent buddies are handled
        enable()
        input.enable()
        crosshair.enable()
        exportedData.enable()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current.scope {
                streamMat(streamId!!, input.value(current).value, input.value(current).color)

                returnMethod(input.value(current).value)
                appendWhiteline = false
            }

            NoSession
        }

        @Suppress("UNCHECKED_CAST")
        generatorFor(CPythonLanguage) {
            current {
                val inputValue = input.value(current)
                val crosshairValue = crosshair.value(current)
                val dataValue = exportedData.value(current)

                current.scope {
                    val llpython = uniqueVariable("llpython", if(dataValue is GenValue.GList.RuntimeListOf<*>) {
                        dataValue.value
                    } else if(dataValue is GenValue.GList.ListOf<*>) {
                        val data = mutableListOf<Value>()

                        for (d in dataValue.elements) {
                            if(d is GenValue.Double) {
                                data.add(d.value.v)
                            }
                        }

                        CPythonLanguage.newArrayOf(CPythonLanguage.NoType, *data.toTypedArray())
                    } else raise("Uh oh, this shouldn't happen"))

                    local(llpython)

                    separate()

                    returnMethod(CPythonLanguage.tuple(crosshairValue.value, inputValue.value, llpython))
                    appendWhiteline = false
                }

                NoSession
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}