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

package io.github.deltacv.papervision.attribute.misc

import com.google.gson.JsonObject
import imgui.ImGui
import imgui.flag.ImGuiCol
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.data.adapter.dataSerializableToJsonObject
import io.github.deltacv.papervision.serialization.data.adapter.jsonObjectToDataSerializable
import io.github.deltacv.papervision.serialization.AttributeSerializationData

open class ListAttribute(
    override val mode: AttributeMode,
    val elementAttributeType: AttributeType,
    override var variableName: String? = null,
    length: Int? = null,
    val allowAddOrDelete: Boolean = true
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.List
        override val allowsNew = false

        override val styleColor = rgbaColor(95, 158, 160, 180)
        override val styleHoveredColor = rgbaColor(95, 158, 160, 255)
    }

    override var icon = "${FontAwesomeIcons.ChevronLeft}[${elementAttributeType.icon}${FontAwesomeIcons.ChevronRight}"

    override val styleColor
        get() = if (elementAttributeType.isDefaultListColor) {
            Companion.styleColor
        } else elementAttributeType.listStyleColor

    override val styleHoveredColor
        get() = if (elementAttributeType.isDefaultListColor) {
            Companion.styleHoveredColor
        } else elementAttributeType.listStyleHoveredColor

    val listAttributes = mutableListOf<TypedAttribute>()
    val deleteQueue = mutableListOf<TypedAttribute>()

    private var lastHasLink = false

    private var lastLength: Int? = 0
    var fixedLength = length
        set(value) {
            field = value
            onEnable()
        }

    private val allowMutation get() = allowAddOrDelete && fixedLength == null

    private var serializationData: Data? = null

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {
        if (serializationData != null) {
            listAttributes.clear()

            for (obj in serializationData!!.attributes) {
                val elem = createElement(false)
                jsonObjectToDataSerializable(obj, inst = elem as DataSerializable<Any>)
                elem.enable()
            }

            serializationData = null
        } else {
            // oh god... (it's been only 10 minutes and i have already forgotten how this works)
            if (lastLength != fixedLength) {
                if (fixedLength != null && (lastLength == null || lastLength == 0)) {
                    repeat(fixedLength!!) {
                        createElement()
                    }
                } else if (lastLength != null) {
                    val delta = (fixedLength ?: 0) - (lastLength ?: 0)

                    if (delta < 0) {
                        repeat(-delta) {
                            val last = listAttributes[listAttributes.size - 1]
                            last.delete()

                            listAttributes.remove(last)
                            deleteQueue.add(last)
                        }
                    } else {
                        repeat(delta) {
                            if (deleteQueue.isNotEmpty()) {
                                val last = deleteQueue[deleteQueue.size - 1]
                                last.restore()

                                listAttributes.add(last)
                                deleteQueue.remove(last)
                            } else {
                                createElement()
                            }
                        }
                    }
                } else {
                    for (attribute in listAttributes.toTypedArray()) {
                        attribute.delete()
                    }
                }
            }
        }

        lastLength = fixedLength
    }

    override fun delete() {
        super.delete()
        for(attribute in listAttributes) {
            attribute.delete()
        }
    }

    override fun restore() {
        super.restore()
        for(attribute in listAttributes) {
            attribute.restore()
        }
    }

    override fun draw() {
        super.draw()

        var ignoreNewLink = false

        // accepts links of elementAttributeType to redirect them into a list element
        if (mode == AttributeMode.INPUT && lastHasLink != hasLink && hasLink && availableLinkedAttribute !is ListAttribute) {
            val linkedAttribute = availableLinkedAttribute!!

            // the user might be crazy and try to link an attribute that is already linked to one of our elements
            // this caused a funny bug during testing, so, please don't do that (not that you can do it anymore)
            val alreadyLinkedAttribute = listAttributes.find {
                it.availableLinkedAttribute == linkedAttribute
            }
            if(alreadyLinkedAttribute == null) {
                createElement(linkTo = linkedAttribute, relatedLink = linkedAttribute.links.last())
            }

            // delete the original link
            // this also handles the case in which the user was indeed crazy
            // and linked an attribute that was already linked to one of our elements
            links.last().delete()

            ignoreNewLink = true
        }

        for ((i, attrib) in listAttributes.withIndex()) {
            if (lastHasLink != hasLink && !ignoreNewLink) {
                if (hasLink) {
                    // delete attributes if a link has been created
                    attrib.delete()
                } else {
                    // restore list attribs if they were previously deleted
                    // after destroying a link with another node
                    attrib.restore()
                }
            }

            if (!hasLink) { // only draw attributes if there's not a link attached
                val isDrawAttributeTextOverridden =
                    drawAttributeText(i, attrib)

                if (isDrawAttributeTextOverridden) {
                    ImGui.sameLine()
                } else {
                    attrib.inputSameLine = true
                }

                attrib.draw()
            }
        }

        lastHasLink = hasLink
    }

    // accept either another ListAttribute with the same element type or a TypedAttribute with the same type as the element type
    override fun acceptLink(other: Attribute) =
        (other is ListAttribute && other.elementAttributeType == elementAttributeType) ||
                (other is TypedAttribute && other.attributeType == elementAttributeType)

    open fun drawAttributeText(index: Int, attrib: Attribute): Boolean = false

    override fun genValue(current: CodeGen.Current): GenValue.GList {
        return if (mode == AttributeMode.INPUT) {
            if (hasLink) {
                val linkedAttrib = availableLinkedAttribute

                raiseAssert(
                    linkedAttrib != null,
                    "List attribute must have another attribute attached"
                )

                val value = linkedAttrib.genValue(current)
                raiseAssert(
                    value is GenValue.GList.ListOf<*> || value is GenValue.GList.RuntimeListOf<*>,
                    "Attribute attached is not a list"
                )

                value
            } else {
                // get the values of all the attributes and return a
                // GenValue.List with the attribute values in an array
                GenValue.GList.List(listAttributes.map { it.genValue(current) }.toTypedArray())
            }
        } else {
            parentNode.genCodeIfNecessary(current)
            val value = getGenValueFromNode(current)
            raiseAssert(
                value is GenValue.GList,
                "Value returned from the node is not a list"
            )

            value
        }
    }

    override fun drawAttribute() {
        super.drawAttribute()

        if (!hasLink && elementAttributeType.allowsNew && allowMutation && mode == AttributeMode.INPUT) {
            // idk wat the frame height is, i just stole it from
            // https://github.com/ocornut/imgui/blob/7b8bc864e9af6c6c9a22125d65595d526ba674c5/imgui_widgets.cpp#L3439

            val buttonSize = ImGui.getFrameHeight()

            val style = ImGui.getStyle()

            ImGui.sameLine(0.0f, style.itemInnerSpacingX * 2.0f)

            if (ImGui.button("+", buttonSize, buttonSize)) {
                // creates a new element with the + button
                // uses the "new" function from the attribute's companion Type
                createElement()
            }

            val hideMinusButton = listAttributes.isEmpty()

            // display the - button only if the attributes list is not empty
            if (hideMinusButton) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0x00000000)
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0x00000000)
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0x00000000)
                ImGui.pushStyleColor(ImGuiCol.Text, 0x00000000)
            }

            ImGui.sameLine(0.0f, style.itemInnerSpacingX)

            if (ImGui.button("-", buttonSize, buttonSize) && !hideMinusButton) {
                // remove the last element from the list when - is pressed
                listAttributes.removeLastOrNull()
                    ?.delete() // also delete it from the element id registry
            }

            if(hideMinusButton) {
                ImGui.popStyleColor()
                ImGui.popStyleColor()
                ImGui.popStyleColor()
                ImGui.popStyleColor()
            }
        }
    }

    override fun readEditorValue(): Array<Any?> {
        val list = mutableListOf<Any?>()

        for(attribute in listAttributes) {
            list.add(attribute.editorValue)
        }

        return list.toTypedArray()
    }

    private fun createElement(enable: Boolean = true, linkTo: Attribute? = null, relatedLink: Link? = null): TypedAttribute {
        val count = listAttributes.size.toString()
        val elementName = count + if (count.length == 1) " " else ""

        val element = elementAttributeType.new(AttributeMode.INPUT, elementName)
        element.parentNode = parentNode
        if(enable) element.enable() //enables the new element

        element.ownedByList = true
        element.drawType = false // hides the variable type
        element.onChange.doPersistent(onChange::run)

        listAttributes.add(element)

        if (linkTo != null) {
            // if the element is being created because of a link, create the link action
            // to link the new element to the linked attribute
            parentNode.editor.onDraw.doOnce {
                val action = CreateLinkAction(Link(linkTo.id, element.id))

                if(relatedLink != null) {
                    val associatedAction = relatedLink.associatedAction
                    if(associatedAction != null) {
                        // sneakily insert ourselves into the action stack by killing and impersonating the original action
                        // that created the original link (hopefully he got the chance to say goodbye to his family)
                        // this avoids glitches when the user tries to undo/redo the creation of this new link
                        // otherwise, the stack would try to address the original link, which doesn't exist anymore
                        associatedAction.idContainer[associatedAction.id] = action
                    }
                }

                action.enable()
            }
        }

        onElementCreation(element)
        return element
    }

    open fun onElementCreation(element: Attribute) {}

    fun forEach(callback: (Attribute) -> Unit) = listAttributes.forEach(callback)

    @JvmName("forEachTyped")
    inline fun <reified T> forEach(callback: (T) -> Unit) = listAttributes.forEach {
        if(it is T) callback(it)
    }

    override fun makeSerializationData(): AttributeSerializationData {
        val objects = mutableListOf<JsonObject>()

        for (attrib in listAttributes) {
            objects.add(dataSerializableToJsonObject(attrib).asJsonObject)
        }

        return Data(objects)
    }

    override fun takeSerializationData(data: AttributeSerializationData) {
        if (data is Data)
            serializationData = data
    }

    data class Data(var attributes: List<JsonObject>) : AttributeSerializationData()

}