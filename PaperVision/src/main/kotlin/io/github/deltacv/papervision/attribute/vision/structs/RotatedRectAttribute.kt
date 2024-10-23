package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons

class RotatedRectAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.VectorSquare

        override fun new(mode: AttributeMode, variableName: String) = RotatedRectAttribute(mode, variableName)
    }

    override fun value(current: CodeGen.Current) = value<GenValue.GRect.Rotated.RuntimeRotatedRect>(
        current, "a Rotated Rect"
    ) { it is GenValue.GRect.Rotated.RuntimeRotatedRect }

}