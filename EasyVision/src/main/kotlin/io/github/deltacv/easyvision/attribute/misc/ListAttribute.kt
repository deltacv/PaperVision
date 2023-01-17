package io.github.deltacv.easyvision.attribute.misc

import com.google.gson.JsonObject
import imgui.ImGui
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.style.rgbaColor
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.data.adapter.dataSerializableToJsonObject
import io.github.deltacv.easyvision.serialization.data.adapter.jsonObjectToDataSerializable
import io.github.deltacv.easyvision.serialization.ev.AttributeSerializationData
import java.util.*

open class ListAttribute(
    override val mode: AttributeMode,
    val elementType: Type,
    override var variableName: String? = null,
    length: Int? = null,
    val allowAddOrDelete: Boolean = true,
    var sameLine: Boolean = false
) : TypedAttribute(Companion) {

    companion object : Type {
        override val name = "List"
        override val allowsNew = false

        override val styleColor = rgbaColor(95, 158, 160, 180)
        override val styleHoveredColor = rgbaColor(95, 158, 160, 255)
    }

    override var typeName = "[${elementType.name}]"

    override val styleColor
        get() = if (elementType.isDefaultListColor) {
            Companion.styleColor
        } else elementType.listStyleColor

    override val styleHoveredColor
        get() = if (elementType.isDefaultListColor) {
            Companion.styleHoveredColor
        } else elementType.listStyleHoveredColor

    val listAttributes = mutableListOf<TypedAttribute>()
    val deleteQueue = mutableListOf<TypedAttribute>()

    private var beforeHasLink = false

    private var previousLength: Int? = 0
    var fixedLength = length
        set(value) {
            field = value
            onEnable()
        }

    private val allowAod get() = allowAddOrDelete && fixedLength == null

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
            if (previousLength != fixedLength) {
                if (fixedLength != null && (previousLength == null || previousLength == 0)) {
                    repeat(fixedLength!!) {
                        createElement()
                    }
                } else if (previousLength != null || previousLength != 0) {
                    val delta = (fixedLength ?: 0) - (previousLength ?: 0)

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

        previousLength = fixedLength
    }

    override fun draw() {
        super.draw()

        for ((i, attrib) in listAttributes.withIndex()) {
            if (beforeHasLink != hasLink) {
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
                isDrawAttributeTextOverriden = true
                drawAttributeText(i, attrib)

                if (isDrawAttributeTextOverriden) {
                    ImGui.sameLine()
                } else {
                    attrib.inputSameLine = true
                }

                attrib.draw()
            }
        }

        beforeHasLink = hasLink
    }

    private var isDrawAttributeTextOverriden = false

    open fun drawAttributeText(index: Int, attrib: Attribute) {
        isDrawAttributeTextOverriden = false
    }

    override fun value(current: CodeGen.Current): GenValue.GList {
        return if (mode == AttributeMode.INPUT) {
            if (hasLink) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "List attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.value(current)
                raiseAssert(
                    value is GenValue.GList.ListOf<*> || value is GenValue.GList.RuntimeListOf<*>,
                    "Attribute attached is not a list"
                )

                value as GenValue.GList
            } else {
                // get the values of all the attributes and return a
                // GenValue.List with the attribute values in an array
                GenValue.GList.List(listAttributes.map { it.value(current) }.toTypedArray())
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(
                value is GenValue.GList,
                "Value returned from the node is not a list"
            )

            value as GenValue.GList
        }
    }

    override fun drawAttribute() {
        super.drawAttribute()

        if (!hasLink && elementType.allowsNew && allowAod && mode == AttributeMode.INPUT) {
            // idk wat the frame height is, i just stole it from
            // https://github.com/ocornut/imgui/blob/7b8bc864e9af6c6c9a22125d65595d526ba674c5/imgui_widgets.cpp#L3439

            val buttonSize = ImGui.getFrameHeight()

            val style = ImGui.getStyle()

            ImGui.sameLine(0.0f, style.itemInnerSpacingX * 2.0f)

            if (ImGui.button("+", buttonSize, buttonSize)) { // creates a new element with the + button
                // uses the "new" function from the attribute's companion Type
                createElement()
            }

            // display the - button only if the attributes list is not empty
            if (listAttributes.isNotEmpty()) {
                ImGui.sameLine(0.0f, style.itemInnerSpacingX)

                if (ImGui.button("-", buttonSize, buttonSize)) {
                    // remove the last element from the list when - is pressed
                    listAttributes.removeLastOrNull()
                        ?.delete() // also delete it from the element id registry
                }
            }
        }
    }

    override fun acceptLink(other: Attribute) = other is ListAttribute && other.elementType == elementType

    override fun thisGet(): Array<Any> {
        val list = mutableListOf<Any>()

        for(attribute in listAttributes) {
            list.add(attribute.get() ?: continue)
        }

        return list.toTypedArray()
    }

    private fun createElement(enable: Boolean = true): TypedAttribute {
        val count = listAttributes.size.toString()
        val elementName = count + if (count.length == 1) " " else ""

        val element = elementType.new(AttributeMode.INPUT, elementName)
        element.parentNode = parentNode
        if(enable) element.enable() //enables the new element

        element.drawType = false // hides the variable type

        element.onChange.doPersistent(onChange::run)

        listAttributes.add(element)

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

    override fun takeDeserializationData(data: AttributeSerializationData) {
        if (data is Data)
            serializationData = data
    }

    data class Data(var attributes: List<JsonObject>) : AttributeSerializationData()

}