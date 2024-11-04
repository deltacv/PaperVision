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

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCol
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.vision.overlay.LineParametersNode

class LineParametersAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(LineParametersAttribute) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.ChartLine

        override fun new(mode: AttributeMode, variableName: String) = LineParametersAttribute(mode, variableName)
    }

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushFont(parentNode.fontAwesome.imfont)

            if(!hasLink && ImGui.button(FontAwesomeIcons.PencilAlt)) {
                val node = parentNode.editor.addNode(LineParametersNode::class.java)

                parentNode.editor.onDraw.doOnce {
                    CreateLinkAction(
                        Link(
                            (node as LineParametersNode).output.id, id
                        )
                    ).enable()

                    node.pinToMouse = true
                    node.pinToMouseOffset = ImVec2(0f, -10f)
                }
            }

            ImGui.popFont()
        }
    }

    override fun value(current: CodeGen.Current): GenValue {
        return if(mode == AttributeMode.INPUT && !hasLink) {
            GenValue.LineParameters.Line(
                GenValue.Scalar(0.0, 255.0, 0.0, 0.0),
                GenValue.Int(3)
            )
        } else {
            value<GenValue.LineParameters>(
                current, "a LineParameters"
            ) { it is GenValue.LineParameters }
        }
    }

}