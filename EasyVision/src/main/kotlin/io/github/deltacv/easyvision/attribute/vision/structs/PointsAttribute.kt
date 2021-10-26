package io.github.deltacv.easyvision.attribute.vision.structs

import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.style.rgbaColor

class PointsAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(MatAttribute) {

    companion object : Type {
        override val name = "Points"

        override val styleColor = rgbaColor(149, 117, 205, 180)
        override val styleHoveredColor = rgbaColor(149, 117, 205, 255)

        override val listStyleColor = rgbaColor(179, 157, 219, 180)
        override val listStyleHoveredColor = rgbaColor(179, 157, 219, 255)

        override fun new(mode: AttributeMode, variableName: String) = PointsAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.GPoints.Points>(
        current, "a Points"
    ) { it is GenValue.GPoints.Points }

}