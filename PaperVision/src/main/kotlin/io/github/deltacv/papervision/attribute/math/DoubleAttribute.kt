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

package io.github.deltacv.papervision.attribute.math

import imgui.ImGui
import imgui.type.ImDouble
import imgui.type.ImFloat
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.id.Misc
import io.github.deltacv.papervision.util.Range2d

class DoubleAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.SquareRootAlt

        override fun new(mode: AttributeMode, variableName: String) = DoubleAttribute(mode, variableName)
    }

    val value = ImDouble()
    private val sliderValue = ImFloat()

    private val sliderId by Misc.newMiscId()

    private var range: Range2d? = null

    override fun drawAttribute() {
        super.drawAttribute()
        checkChange()

        if(!hasLink && mode == AttributeMode.INPUT) {
            sameLineIfNeeded()

            ImGui.pushItemWidth(110.0f)

            if(range == null) {
                ImGui.inputDouble("", value)
            } else {
                ImGui.sliderFloat("###$sliderId", sliderValue.data, range!!.min.toFloat(), range!!.max.toFloat())
                value.set(sliderValue.get().toDouble())
            }

            ImGui.popItemWidth()
        }
    }

    fun sliderMode(range: Range2d) {
        this.range = range
    }

    fun normalMode() {
        this.range = null
    }

    override fun readEditorValue() = value.get()

    override fun genValue(current: CodeGen.Current) = value(
        current, "a Double", GenValue.Double(value.get().resolved())
    ) { it is GenValue.Double }

}