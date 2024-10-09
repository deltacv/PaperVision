package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.style.rgbaColor

class PointsAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(PointsAttribute) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.DotCircle

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