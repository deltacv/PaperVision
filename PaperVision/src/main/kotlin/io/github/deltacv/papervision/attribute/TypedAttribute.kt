/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

interface AttributeType {
    val icon: String
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

abstract class TypedAttribute(val attributeType: AttributeType) : Attribute() {

    abstract var variableName: String?

    open val styleColor get() = attributeType.styleColor
    open val styleHoveredColor get() = attributeType.styleHoveredColor

    open val linkColor get() = styleColor
    open val linkHoveredColor get() = styleHoveredColor

    var drawDescriptiveText = true
    var drawType = true

    var inputSameLine = false

    open var icon = attributeType.icon

    open var drawAfterTextSize = ImVec2()
        protected set

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
            val t = tr(finalVarName)

            if(mode == AttributeMode.INPUT) {
                ImGui.pushFont(parentNode.fontAwesome.imfont)
                ImGui.text(icon)
                ImGui.popFont()

                ImGui.sameLine()

                ImGui.text(t)

                drawAfterText()
            } else {
                val textSize = ImGui.calcTextSize(t)

                ImGui.pushFont(parentNode.fontAwesome.imfont)
                textSize.plus(ImGui.calcTextSize(icon))
                ImGui.popFont()

                textSize.plus(drawAfterTextSize)

                if(parentNode.nodeAttributes.size > 1) {
                    ImGui.indent(nodeSize.x - (textSize.x))
                } else {
                    ImGui.indent(textSize.x * 0.6f)
                }

                ImGui.text(t)
                ImGui.sameLine()

                ImGui.pushFont(parentNode.fontAwesome.imfont)
                ImGui.text(icon)
                ImGui.popFont()

                drawAfterText()
            }
        } else if(!inputSameLine) {
            ImGui.text("")
        } else {
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
                val linkedAttrib = enabledLinkedAttribute()

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

    protected var cachedLabel: String? = null

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