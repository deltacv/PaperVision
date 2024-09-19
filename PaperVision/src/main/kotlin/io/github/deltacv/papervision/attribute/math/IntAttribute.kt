package io.github.deltacv.papervision.attribute.math

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.Type
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.serialization.AttributeSerializationData
import io.github.deltacv.papervision.util.Range2i

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

    private var range: Range2i? = null
    private var sliders = false

    private val sliderId by PaperVision.miscIds.nextId()
    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            if(!sliders) {
                range?.let {
                    value.set(it.clip(value.get()))
                }

                ImGui.inputInt("", value, 1, 100, if(disableInput) ImGuiInputTextFlags.ReadOnly else 0)
            } else {
                sliderValue.set(range!!.clip(sliderValue.get()))

                ImGui.sliderInt("###$sliderId", sliderValue.data, range!!.min, range!!.max)
                value.set(sliderValue.get())
            }

            if(!ImGui.isItemFocused()) {
                checkChange()
            }

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
        sliders = true
    }

    fun normalMode(range: Range2i? = null) {
        this.range = range
        sliders = false
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