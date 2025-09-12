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

package io.github.deltacv.papervision.attribute.vision

import imgui.ImGui
import imgui.flag.ImGuiCol
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.display.ImageDisplayNode
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.gui.util.ExtraWidgets
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    var allowPrevizButton: Boolean = false
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.Image

        override val styleColor = rgbaColor(0, 151, 167, 180)
        override val styleHoveredColor = rgbaColor(0, 151, 167, 255)

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

    @SerializeIgnore
    var isPrevizEnabled = false
        private set

    private var prevIsPrevizEnabled = false

    @SerializeIgnore
    var wasPrevizJustEnabled = false
        private set

    @SerializeIgnore
    var displayWindow: ImageDisplayNode? = null
        private set

    override fun drawAfterText() {
        if(mode == AttributeMode.OUTPUT && allowPrevizButton && isOnEditor) {
            ImGui.sameLine()

            ImGui.pushFont(editor.fontAwesome.imfont)
                val text = if (isPrevizEnabled) FontAwesomeIcons.EyeSlash else FontAwesomeIcons.Eye

                ImGui.pushStyleColor(ImGuiCol.Button, 0)

                isPrevizEnabled = ExtraWidgets.toggleButton(
                    text, isPrevizEnabled
                )

                drawAfterTextSize = ImGui.getItemRectSize()

                ImGui.popStyleColor()
            ImGui.popFont()
        }

        val wasButtonToggled = (isPrevizEnabled != prevIsPrevizEnabled)
        wasPrevizJustEnabled = wasButtonToggled && isPrevizEnabled

        if(wasPrevizJustEnabled) {
            displayWindow = editor.startImageDisplayFor(this)

            displayWindow!!.onDelete.doOnce {
                isPrevizEnabled = false
                displayWindow = null
            }
        } else if(wasButtonToggled) {
            displayWindow?.delete()
            displayWindow = null
        }

        if(wasButtonToggled) {
            editor.onDraw.doOnce {
                onChange.run()
            }
        }

        prevIsPrevizEnabled = isPrevizEnabled
    }

    override fun value(current: CodeGen.Current) = value<GenValue.Mat>(
        current, "a Mat"
    ) { it is GenValue.Mat }

    fun enablePrevizButton() = apply { allowPrevizButton = true }

    override fun restore() {
        super.restore()

        isPrevizEnabled = false
        prevIsPrevizEnabled = false
        displayWindow = null
    }

}