package io.github.deltacv.easyvision.attribute.vision.structs

import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue

class RectAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : Type {
        override val name = "Rect"

        override fun new(mode: AttributeMode, variableName: String) = RectAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.GRect.RuntimeRect>(
        current, "a Rect"
    ) { it is GenValue.GRect.RuntimeRect }

}