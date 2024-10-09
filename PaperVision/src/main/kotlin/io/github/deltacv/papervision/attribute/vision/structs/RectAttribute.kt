package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons

class RectAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.Square

        override fun new(mode: AttributeMode, variableName: String) = RectAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.GRect.RuntimeRect>(
        current, "a Rect"
    ) { it is GenValue.GRect.RuntimeRect }

}