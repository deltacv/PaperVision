package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons

class CircleAttribute(
    override val mode: AttributeMode,
    override var variableName: String?
) : TypedAttribute(CircleAttribute) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.Circle

        override val styleColor = rgbaColor(253, 216, 53, 180)
        override val styleHoveredColor = rgbaColor(253, 216, 53, 255)

        override val listStyleColor = rgbaColor(253, 216, 53, 140)
        override val listStyleHoveredColor = rgbaColor(253, 216, 53, 255)

        override fun new(mode: AttributeMode, variableName: String) = KeyPointsAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.GCircle>(
        current, "a Circle"
    ) { it is GenValue.GCircle }

}