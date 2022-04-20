package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.serialization.data.SerializeData
import io.github.deltacv.easyvision.serialization.ev.AttributeSerializationData
import io.github.deltacv.easyvision.util.Range2i

class IntAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Int"

        override fun new(mode: AttributeMode, variableName: String) = IntAttribute(mode, variableName)
    }

    val value = ImInt()
    private val sliderValue = ImInt()
    private var nextValue: Int? = null

    var disableInput = false
        set(value) {
            showAttributesCircles = !value
            field = value
        }

    @SerializeData
    private var range: Range2i? = null

    private val sliderId by EasyVision.miscIds.nextId()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            if(range == null) {
                ImGui.inputInt("", value, 1, 100, if(disableInput) ImGuiInputTextFlags.ReadOnly else 0)
            } else {
                ImGui.sliderInt("###$sliderId", sliderValue.data, range!!.min, range!!.max)
                value.set(sliderValue.get())
            }

            checkChange()

            ImGui.popItemWidth()

            if(nextValue != null) {
                value.set(nextValue!!)
                sliderValue.set(nextValue!!)
                nextValue = null
            }
        }
    }

    fun sliderMode(range: Range2i) {
        this.range = range
    }

    fun normalMode() {
        this.range = null
    }

    override fun thisGet() = value.get()

    override fun value(current: CodeGen.Current) = value(
        current, "an Int", GenValue.Int(value.get())
    ) { it is GenValue.Int }

    override fun makeSerializationData() = Data(value.get())

    override fun takeDeserializationData(data: AttributeSerializationData) {
        if(data is Data) {
            nextValue = data.value
        }
    }

    data class Data(var value: Int = 0) : AttributeSerializationData()

}