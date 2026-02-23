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
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.engine.client.message.TunerChangeValueMessage
import io.github.deltacv.papervision.engine.client.message.TunerValue
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
        throw UnsupportedOperationException("Cannot instantiate this attribute with new")
    }

    fun newDecomposer(): AttributeDecomposer<*>? = null
}

abstract class TypedAttribute<R: GenValue>(
    val attributeType: AttributeType<*>,
    val doLinkChangeChecking: Boolean = true,
    val doEditorChangeChecking: Boolean = false
) : Attribute() {

    abstract var variableName: String?

    open val styleColor get() = attributeType.styleColor
    open val styleHoveredColor get() = attributeType.styleHoveredColor

    open val linkColor get() = styleColor
    open val linkHoveredColor get() = styleHoveredColor

    var drawDescriptiveText = true
    var drawType = true

    var inlineInput = false

    open var icon = attributeType.icon

    open var drawAfterTextSize = ImVec2()
        protected set

    private var isFirstDraw = true
    private var isSecondDraw = false

    private var cachedLabels = mutableMapOf<Int?, String>()

    private val finalVarName by lazy {
        variableName ?: if (mode == AttributeMode.INPUT) "$[mis_input]" else "$[mis_output]"
    }

    val nodeSize = ImVec2()

    private var previousLinkedAttributes: List<Attribute?> = emptyList()
    private var previousGet: Any? = null

    private val monospaceFont by Font.findLazy("jetbrains-mono")
    private val fontAwesome by Font.findLazy("font-awesome")

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

        if(inlineInput) {
            ImGui.pushFont(monospaceFont.imfont)
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
        } else if(!inlineInput) {
            ImGui.text("")
        } else {
            drawAfterText()
        }

        if(inlineInput) {
            ImGui.popFont()
        }

        if(doLinkChangeChecking) {
            val currentLinkedAttribs = availableLinkedAttributes

            var linksChanged = false

            if (previousLinkedAttributes.size != currentLinkedAttribs.size) {
                linksChanged = true
            } else {
                outer@ for (previousLink in previousLinkedAttributes) {
                    for (currentLink in currentLinkedAttribs) {
                        if (previousLink === currentLink) continue@outer
                    }
                    linksChanged = true
                    break
                }
            }

            if (linksChanged) {
                emitChange(ChangeType.LinkChange)
            }

            previousLinkedAttributes = currentLinkedAttribs.toList()
        }

        if(doEditorChangeChecking) {
            val currentGet = editorValue

            if(currentGet != previousGet) {
                emitChange(ChangeType.ValueChange)
            }

            previousGet = currentGet
        }
    }

    open fun drawAfterText() { }

    protected fun inlineIfNeeded() {
        if(inlineInput) {
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
                raiseAssert(value is R, tr("err_attachedattrib_isnot", R::class.simpleName ?: "unknown"))

                value
            } else {
                inputFieldValue
            }
        } else {
            val value = getGenValueFromNode(current)
            raiseAssert(value is R, tr("err_valreturned_isnot", R::class.simpleName ?: "unknown"))

            return value
        }
    }

    // acceptLink is overridden to allow for ListAttribute to accept TypedAttribute
    override fun acceptLink(other: Attribute): LinkAcceptance {
        // basic type check
        val sameTypedAttribute =
            other is TypedAttribute<*> && other.attributeType == attributeType

        val sameClass =
            this::class == other::class

        // we'll let the AnyAttribute determine if it can link or not
        val outputToAny =
            mode == AttributeMode.OUTPUT && other is AnyAttribute

        // allow linking to ListAttribute if the types are the same so it becomes an element of the list
        // ONLY IF THIS ELEMENT IS OUTPUT otherwise it allows to link a list output to an individual input
        val outputToMatchingList =
            mode == AttributeMode.OUTPUT &&
                    other is ListAttribute<*, *> &&
                    other.elementAttributeType == attributeType

        return if (
            sameTypedAttribute ||
            sameClass ||
            outputToAny ||
            outputToMatchingList
        ) {
            LinkAcceptance.Accept
        } else {
            LinkAcceptance.Reject()
        }
    }

    open fun readEditorValue(): Any? = null
    open fun readTunerValue(): TunerValue? = null

    override val editorValue get() = when {
        mode == AttributeMode.INPUT -> readEditorValue()
        else -> null
    }
    override val tunerValue get() = when {
        mode == AttributeMode.INPUT -> readTunerValue()
        else -> null
    }

    fun label(indexIfApplicable: Int? = null): String {
        if(!cachedLabels.containsKey(indexIfApplicable)) {
            val label = id.toString() + (indexIfApplicable?.let { "_$it" } ?: "")
            cachedLabels[indexIfApplicable] = label

            onChange {
                val value = tunerValue

                // if value is null we have an oopsie and we should just rebuild
                // always rebuild on link changes, no other way to handle it
                if(value == null || peekChange() is ChangeType.LinkChange) {
                    rebuildPreviz()
                    return@onChange
                }

                broadcastLabelMessageFor(label, value, indexIfApplicable)
            }
        }

        return cachedLabels[indexIfApplicable]!!
    }

    protected fun broadcastLabelMessageFor(label: String, value: TunerValue, indexIfApplicable: Int? = null) {
        if(!isOnEditor) return

        parentNode.editor.paperVision.engineClient.sendMessage(
            when (value) {
                is TunerValue.ListValue -> {
                    if(indexIfApplicable != null) {
                        TunerChangeValueMessage(label, value.values.getOrNull(indexIfApplicable)
                            ?: throw IndexOutOfBoundsException("Index $indexIfApplicable is out of bounds for list of size ${value.values.size}")
                        )
                    } else {
                        TunerChangeValueMessage(label, value)
                    }
                }
                else -> TunerChangeValueMessage(label, value)
            }
        )
    }

}
