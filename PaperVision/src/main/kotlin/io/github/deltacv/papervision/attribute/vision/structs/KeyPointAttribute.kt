package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.gui.style.rgbaColor

class KeyPointAttribute(override val mode: AttributeMode,
                        override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.PlusCircle

        override val styleColor = rgbaColor(253, 216, 53, 180)
        override val styleHoveredColor = rgbaColor(253, 216, 53, 255)

        override val listStyleColor = rgbaColor(253, 216, 53, 140)
        override val listStyleHoveredColor = rgbaColor(253, 216, 53, 255)

        override fun new(mode: AttributeMode, variableName: String) = KeyPointAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.GKeyPoint.RuntimeKeyPoint>(
        current, "a KeyPoint"
    ) { it is GenValue.GKeyPoint.RuntimeKeyPoint }

}