package io.github.deltacv.papervision.attribute.math

import imgui.ImGui
import imgui.type.ImBoolean
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.serialization.data.SerializeData

class BooleanAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.ToggleOn

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

    override fun thisGet() = value.get()

    override fun value(current: CodeGen.Current): GenValue.Boolean {
        if(isInput) {
            if(hasLink) {
                val linkedAttrib = enabledLinkedAttribute()

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