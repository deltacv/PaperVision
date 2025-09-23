/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.attribute.vision.structs

import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.style.rgbaColor

class RotatedRectAttribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.VectorSquare

        override val styleColor = rgbaColor(253, 216, 53, 180)
        override val styleHoveredColor = rgbaColor(253, 216, 53, 255)

        override val listStyleColor = rgbaColor(253, 216, 53, 140)
        override val listStyleHoveredColor = rgbaColor(253, 216, 53, 255)

        override fun new(mode: AttributeMode, variableName: String) = RotatedRectAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.GRect.Rotated.RuntimeRotatedRect>(
        current, "a Rotated Rect"
    ) { it is GenValue.GRect.Rotated.RuntimeRotatedRect }

}