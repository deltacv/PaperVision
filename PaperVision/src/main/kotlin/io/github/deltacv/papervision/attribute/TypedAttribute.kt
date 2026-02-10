/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
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
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.engine.client.message.TunerChangeValueMessage
import io.github.deltacv.papervision.engine.client.message.TunerChangeValuesMessage
import io.github.deltacv.papervision.gui.util.Font

interface AttributeType<A: TypedAttribute<*>> {
    val icon: String
    val allowsNew: Boolean get() = true

    val styleColor: Int get() = PaperVision.imnodesStyle.pin
    val styleHoveredColor: Int get() = PaperVision.imnodesStyle.pinHovered

    val listStyleColor: Int get() = PaperVision.imnodesStyle.pin
    val listStyleHoveredColor: Int get() = PaperVision.imnodesStyle.pinHovered

    val isDefaultListColor: Boolean get() =
        listStyleColor == PaperVision.imnodesStyle.pin
            && listStyleHoveredColor == PaperVision.imnodesStyle.pinHovered

    fun new(mode: AttributeMode, variableName: String): A {
        throw UnsupportedOperationException("Cannot instantiate a List attribute with new")
    }
}

abstract class TypedAttribute<R: GenValue>(val attributeType: AttributeType<*>) : Attribute() {

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

    var ownedByList = false
        internal set

    private var previousGet: Any? = null
    private var cachedLabels = mutableMapOf<Int?, String>()

    private val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "$[mis_input]" else "$[mis_output]"
    }

    val nodeSize = ImVec2()

    private val defaultImGuiFont = Font.find("default-12")
    private val fontAwesome = Font.find("font-awesome")

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
            ImGui.pushFont(defaultImGuiFont.imfont)
        }

        if(drawDescriptiveText) {
            val t = tr(finalVarName)

            if(mode == AttributeMode.INPUT) {
                ImGui.pushFont(fontAwesome.imfont)
                ImGui.text(icon)
                ImGui.popFont()

                ImGui.sameLine()

                ImGui.text(t)

                drawAfterText()
            } else {
                val textSize = ImGui.calcTextSize(t)

                ImGui.pushFont(fontAwesome.imfont)
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

                ImGui.pushFont(fontAwesome.imfont)
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

    open fun drawAfterText() { }

    protected fun sameLineIfNeeded() {
        if(inputSameLine) {
            ImGui.sameLine()
        }
    }

    abstract override fun genValue(current: CodeGen.Current): R

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified R : GenValue> readGenValue(
        current: CodeGen.Current,
        inputFieldValue: R? = null
    ): R {
        if(isInput) {
            return if(hasLink || inputFieldValue == null) {
                val linkedAttrib = availableLinkedAttribute

                raiseAssert(
                    linkedAttrib != null,
                    tr("err_musthave_attachedattrib")
                )

                val value = linkedAttrib.genValue(current)
                raiseAssert(value is R, tr("err_attachedattrib_isnot", this::class.java.simpleName))

                value
            } else {
                inputFieldValue
            }
        } else {
            val value = getGenValueFromNode(current)
            raiseAssert(value is R, tr("err_valreturned_isnot", this::class.java.simpleName))

            return value
        }
    }

    // acceptLink is overridden to allow for ListAttribute to accept TypedAttribute
    override fun acceptLink(other: Attribute) =
        (other is TypedAttribute<*> && other.attributeType == attributeType) ||
            this::class == other::class ||
                // allow linking to ListAttribute if the types are the same so it becomes an element of the list
                // ONLY IF THIS ELEMENT IS OUTPUT otherwise it allows to link a list output to an individual input
                (other is ListAttribute<*, *> && other.elementAttributeType == attributeType && mode == AttributeMode.OUTPUT)

    protected fun changed() {
        if (!isFirstDraw && !isSecondDraw) onChange.run()
    }

    /**
     * MUST be called if you want to get change detection for input attributes
     */
    protected open fun checkChange() {
        if(mode == AttributeMode.INPUT) {
            val currentGet = editorValue

            if (currentGet != previousGet) {
                changed()
            }

            previousGet = currentGet
        }
    }

    fun label(indexIfApplicable: Int? = null): String {
        if(!cachedLabels.containsKey(indexIfApplicable)) {
            val label = id.toString() + (indexIfApplicable?.let { "_$it" } ?: "")
            cachedLabels[indexIfApplicable] = label

            onChange {
                val value = editorValue

                if(value == null) {
                    rebuildPreviz()
                    return@onChange
                }

                broadcastLabelMessageFor(label, value, indexIfApplicable)
            }
        }

        return cachedLabels[indexIfApplicable]!!
    }

    protected fun broadcastLabelMessageFor(label: String, value: Any, indexIfApplicable: Int? = null) {
        if(!isOnEditor) return

        parentNode.editor.paperVision.engineClient.sendMessage(
            when (value) {
                is Array<*> -> if(indexIfApplicable != null) {
                    TunerChangeValueMessage(label, value[indexIfApplicable] as Any) // only send the specific index that we want
                } else TunerChangeValuesMessage(label, value) // send all values

                is Iterable<*> -> if(indexIfApplicable != null) {
                    TunerChangeValueMessage(label, value.elementAt(indexIfApplicable) as Any) // only send the specific index that we want
                } else TunerChangeValuesMessage(label, value.map { it as Any }.toTypedArray()) // send all values

                else -> TunerChangeValueMessage(label, value) // bleh
            }
        )
    }

}
