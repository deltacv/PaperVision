package io.github.deltacv.easyvision.attribute

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesColorStyle
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.util.hexString
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValueMessage
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValuesMessage
import io.github.deltacv.mai18n.tr

interface Type {
    val name: String
    val allowsNew: Boolean get() = true

    val styleColor: Int get() = EasyVision.imnodesStyle.pin
    val styleHoveredColor: Int get() = EasyVision.imnodesStyle.pinHovered

    val listStyleColor: Int get() = EasyVision.imnodesStyle.pin
    val listStyleHoveredColor: Int get() = EasyVision.imnodesStyle.pinHovered

    val isDefaultListColor: Boolean get() =
        listStyleColor == EasyVision.imnodesStyle.pin
            && listStyleHoveredColor == EasyVision.imnodesStyle.pinHovered

    fun new(mode: AttributeMode, variableName: String): TypedAttribute {
        throw UnsupportedOperationException("Cannot instantiate a List attribute with new")
    }
}

abstract class TypedAttribute(val type: Type) : Attribute() {

    abstract var variableName: String?

    open val styleColor get() = type.styleColor
    open val styleHoveredColor get() = type.styleHoveredColor

    open val linkColor get() = styleColor
    open val linkHoveredColor get() = styleHoveredColor

    var drawDescriptiveText = true
    var drawType = true

    var inputSameLine = false

    open var typeName = "(${type.name})"

    private var isFirstDraw = true
    private var isSecondDraw = false

    private val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "$[mis_input]" else "$[mis_output]"
    }

    val nodeSize = ImVec2()

    override fun draw() {
        ImNodes.pushColorStyle(ImNodesColorStyle.Pin, styleColor)
        ImNodes.pushColorStyle(ImNodesColorStyle.PinHovered, styleHoveredColor)

        super.draw()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()
    }

    override fun drawAttribute() {
        if(isSecondDraw) {
            ImNodes.getNodeDimensions(parentNode.id, nodeSize)

            isSecondDraw = false
        }

        if(isFirstDraw) {
            isSecondDraw = true
            isFirstDraw = false
        }

        if(inputSameLine) {
            ImGui.pushFont(EasyVision.defaultImGuiFont.imfont)
        }

        if(drawDescriptiveText) {
            val t: String

            if(mode == AttributeMode.OUTPUT && parentNode.nodeAttributes.size > 1) {
                t = tr(if(drawType) {
                    "$finalVarName $typeName"
                } else finalVarName)

                val textSize = ImVec2()
                ImGui.calcTextSize(textSize, t)

                ImGui.indent((nodeSize.x - textSize.x))
            } else {
                t = tr(if(drawType) {
                    "$typeName $finalVarName"
                } else finalVarName)
            }

            ImGui.text(t)
        } else if(!inputSameLine) {
            ImGui.text("")
        }

        if(inputSameLine) {
            ImGui.popFont()
        }
    }

    protected fun sameLineIfNeeded() {
        if(inputSameLine) {
            ImGui.sameLine()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : GenValue> value(
        current: CodeGen.Current,
        name: String,
        inputFieldValue: T? = null,
        checkConsumer: (GenValue) -> Boolean
    ): T {
        if(isInput) {
            return if(hasLink || inputFieldValue == null) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    tr("err_musthave_attachedattrib", name)
                )

                val value = linkedAttrib!!.value(current)
                raiseAssert(checkConsumer(value), tr("err_attachedattrib_isnot", name))

                value as T
            } else {
                inputFieldValue
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(checkConsumer(value), tr("err_valreturned_isnot", name))

            return value as T
        }
    }

    override fun acceptLink(other: Attribute) = this::class == other::class

    protected fun changed() {
        if(!isFirstDraw && !isSecondDraw) onChange.run()
    }

    private var previousGet: Any? = null

    protected fun checkChange() {
        if(mode == AttributeMode.INPUT) {
            val currentGet = get()

            if (currentGet != previousGet) {
                changed()
            }

            previousGet = currentGet
        }
    }

    private var cachedLabel: String? = null

    @Suppress("UNCHECKED_CAST")
    open fun label(): String {
        if(cachedLabel == null) {
            cachedLabel = hexString

            onChange {
                val value = getIfPossible { rebuildPreviz() } ?: return@onChange
                broadcastLabelMessageFor(cachedLabel!!, value)
            }
        }

        return cachedLabel!!
    }

    protected fun broadcastLabelMessageFor(label: String, value: Any) =
        EasyVision.eocvSimIpcClient.broadcastIfPossible(
            when (value) {
                is Array<*> -> TunerChangeValuesMessage(label, value)
                is Iterable<*> -> TunerChangeValuesMessage(label, value.map { it as Any }.toTypedArray())
                else -> TunerChangeValueMessage(label, 0, value)
            }
        )

}