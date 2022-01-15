package io.github.deltacv.easyvision.attribute.math

import imgui.ImGui
import imgui.type.ImBoolean
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.serialization.data.SerializeData

class BooleanAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Boolean"

        override fun new(mode: AttributeMode, variableName: String) = BooleanAttribute(mode, variableName)
    }

    @SerializeData
    val value = ImBoolean()

    override fun drawAttribute() {
        super.drawAttribute()
        checkChange()

        if(!hasLink && mode == AttributeMode.INPUT) {
            ImGui.checkbox("", value)
        }
    }

    override fun get() = value.get()

    override fun value(current: CodeGen.Current): GenValue.Boolean {
        if(isInput) {
            if(hasLink) {
                val linkedAttrib = linkedAttribute()

                raiseAssert(
                    linkedAttrib != null,
                    "Boolean attribute must have another attribute attached"
                )

                val value = linkedAttrib!!.value(current)
                raiseAssert(value is GenValue.Boolean, "Attribute attached is not a Boolean")

                return value as GenValue.Boolean
            } else {
                return if (value.get()) {
                    GenValue.Boolean.True
                } else GenValue.Boolean.False
            }
        } else {
            val value = getOutputValue(current)
            raiseAssert(value is GenValue.Boolean, "Value returned from the node is not a Boolean")

            return value as GenValue.Boolean
        }
    }

}