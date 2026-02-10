/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
) : TypedAttribute<GenValue.GKeyPoint>(Companion) {

    companion object : AttributeType<KeyPointAttribute> {
        override val icon = FontAwesomeIcons.PlusCircle

        override val styleColor = rgbaColor(253, 216, 53, 180)
        override val styleHoveredColor = rgbaColor(253, 216, 53, 255)

        override val listStyleColor = rgbaColor(253, 216, 53, 140)
        override val listStyleHoveredColor = rgbaColor(253, 216, 53, 255)

        override fun new(mode: AttributeMode, variableName: String) = KeyPointAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.GKeyPoint.RuntimeKeyPoint>(current)

}
