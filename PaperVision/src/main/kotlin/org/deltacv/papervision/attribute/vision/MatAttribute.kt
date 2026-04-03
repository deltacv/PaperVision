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

package org.deltacv.papervision.attribute.vision

import imgui.ImGui
import org.deltacv.papervision.attribute.TypedAttribute
import org.deltacv.papervision.attribute.AttributeMode
import org.deltacv.papervision.attribute.AttributeType
import org.deltacv.papervision.attribute.decomp.vision.MatAttributeDecomposer
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.gui.display.ImageDisplayNode
import org.deltacv.papervision.gui.style.rgbaColor
import org.deltacv.papervision.gui.util.ImGuiEx
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.serialization.v1.data.SerializeIgnore
import org.deltacv.papervision.serialization.v2.CodecType

@CodecType(instantiable = false)
class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    var allowPrevizButton: Boolean = false
) : TypedAttribute<GenValue.Mat>(Companion) {

    companion object: AttributeType<MatAttribute> {
        override val icon = FontAwesomeIcons.Image

        override val styleColor = rgbaColor(0, 151, 167, 180)
        override val styleHoveredColor = rgbaColor(0, 151, 167, 255)

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)

        override fun newDecomposer() = MatAttributeDecomposer()
    }

    @field:SerializeIgnore
    var isPrevizEnabled = false
        private set

    private var prevIsPrevizEnabled = false

    @field:SerializeIgnore
    var wasPrevizJustEnabled = false
        private set

    @field:SerializeIgnore
    var displayWindow: ImageDisplayNode? = null
        private set

    private val fontAwesome by Font.findLazy("font-awesome")

    override fun drawAfterText() {
        if(mode == AttributeMode.OUTPUT && allowPrevizButton && isOnEditor) {
            ImGui.sameLine()

            ImGui.pushFont(fontAwesome.imfont)
                val text = if (isPrevizEnabled) FontAwesomeIcons.EyeSlash else FontAwesomeIcons.Eye

                isPrevizEnabled = ImGuiEx.toggleButton(
                    text, isPrevizEnabled
                )

                drawAfterTextSize = ImGui.getItemRectSize()
            ImGui.popFont()
        }

        val wasButtonToggled = (isPrevizEnabled != prevIsPrevizEnabled)
        wasPrevizJustEnabled = wasButtonToggled && isPrevizEnabled

        if(wasPrevizJustEnabled) {
            displayWindow = editor.startImageDisplayFor(this)

            displayWindow!!.onDelete.once {
                isPrevizEnabled = false
                displayWindow = null
            }
        } else if(wasButtonToggled) {
            displayWindow?.delete()
            displayWindow = null
        }

        if(wasButtonToggled) {
            editor.onDraw.once {
                onChange.run()
            }
        }

        prevIsPrevizEnabled = isPrevizEnabled
    }

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.Mat>(current)

    fun enablePrevizButton() = apply { allowPrevizButton = true }

    override fun restore() {
        super.restore()

        isPrevizEnabled = false
        prevIsPrevizEnabled = false
        displayWindow = null
    }

}



