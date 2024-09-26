package io.github.deltacv.papervision.attribute

import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.util.hexString
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.engine.client.message.TunerChangeValueMessage
import io.github.deltacv.papervision.engine.client.message.TunerChangeValuesMessage

interface Type {
    val name: String
    val allowsNew: Boolean get() = true

    val styleColor: Int get() = PaperVision.imnodesStyle.pin
    val styleHoveredColor: Int get() = PaperVision.imnodesStyle.pinHovered

    val listStyleColor: Int get() = PaperVision.imnodesStyle.pin
    val listStyleHoveredColor: Int get() = PaperVision.imnodesStyle.pinHovered

    val isDefaultListColor: Boolean get() =
        listStyleColor == PaperVision.imnodesStyle.pin
            && listStyleHoveredColor == PaperVision.imnodesStyle.pinHovered

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
        ImNodes.pushColorStyle(ImNodesCol.Pin, styleColor)
        ImNodes.pushColorStyle(ImNodesCol.PinHovered, styleHoveredColor)

        super.draw()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()
    }

    override fun drawAttribute() {
        if(isSecondDraw) {
            ImNodes.getNodeDimensions(nodeSize, parentNode.id)

            isSecondDraw = false
        }

        if(isFirstDraw) {
            isSecondDraw = true
            isFirstDraw = false
        }

        if(inputSameLine) {
            ImGui.pushFont(PaperVision.defaultImGuiFont.imfont)
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

            if(mode == AttributeMode.INPUT) {
                drawAfterText()
            }

            ImGui.text(t)
        } else if(!inputSameLine) {
            ImGui.text("")
        } else if(mode == AttributeMode.INPUT) {
            drawAfterText()
        }

        if(mode == AttributeMode.OUTPUT) {
            drawAfterText()
        }

        if(inputSameLine) {
            ImGui.popFont()
        }
    }

    open fun drawAfterText() {
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
                    tr("err_musthave_attachedattrib")
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

    protected open fun checkChange() {
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

    protected fun broadcastLabelMessageFor(label: String, value: Any) {
        if(!isOnEditor) return

        parentNode.editor.paperVision.engineClient.sendMessage(
            when (value) {
                is Array<*> -> TunerChangeValuesMessage(label, value)
                is Iterable<*> -> TunerChangeValuesMessage(label, value.map { it as Any }.toTypedArray())
                else -> TunerChangeValueMessage(label, 0, value)
            }
        )
    }

}