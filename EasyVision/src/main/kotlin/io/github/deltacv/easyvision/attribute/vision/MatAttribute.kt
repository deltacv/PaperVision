package io.github.deltacv.easyvision.attribute.vision

import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.style.rgbaColor

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Image"

        override val styleColor = rgbaColor(255, 213, 79, 180)
        override val styleHoveredColor = rgbaColor(255, 213, 79, 255)

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.Mat>(
        current, "a Mat"
    ) { it is GenValue.Mat }

}