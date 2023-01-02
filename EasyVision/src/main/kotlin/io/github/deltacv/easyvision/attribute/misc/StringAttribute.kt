package io.github.deltacv.easyvision.attribute.misc

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImInt
import imgui.type.ImString
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.serialization.data.SerializeData
import io.github.deltacv.easyvision.serialization.ev.AttributeSerializationData
import io.github.deltacv.easyvision.util.Range2i

class StringAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "String"

        override fun new(mode: AttributeMode, variableName: String) = StringAttribute(mode, variableName)
    }

    val value = ImString()

    private var nextValue: String? = null

    var disableInput = false
        set(value) {
            showAttributesCircles = !value
            field = value
        }

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            ImGui.inputText("", value, if(disableInput) ImGuiInputTextFlags.ReadOnly else 0)

            if(!ImGui.isItemFocused()) {
                checkChange()
            }

            ImGui.popItemWidth()

            if(nextValue != null) {
                value.set(nextValue!!)
                nextValue = null
            }
        }
    }

    override fun thisGet() = value.get()

    override fun value(current: CodeGen.Current) = value(
        current, "a String", GenValue.String(value.get())
    ) { it is GenValue.String }

    override fun makeSerializationData() = Data(value.get())

    override fun takeDeserializationData(data: AttributeSerializationData) {
        if(data is Data) {
            nextValue = data.value
        }
    }

    data class Data(var value: String = "") : AttributeSerializationData()

}