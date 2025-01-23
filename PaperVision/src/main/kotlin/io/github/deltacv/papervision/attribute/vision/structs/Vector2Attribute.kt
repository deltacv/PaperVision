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
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.math.Vector2Node

class Vector2Attribute (
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.DotCircle

        override fun new(mode: AttributeMode, variableName: String) = Vector2Attribute(mode, variableName)
    }

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushFont(parentNode.fontAwesome.imfont)

            if(!hasLink && ImGui.button(FontAwesomeIcons.PencilAlt)) {
                val node = parentNode.editor.addNode(Vector2Node::class.java)

                parentNode.editor.onDraw.doOnce {
                    CreateLinkAction(
                        Link(
                            (node as Vector2Node).result.id, id
                        )
                    ).enable()

                    node.pinToMouse = true
                    node.pinToMouseOffset = ImVec2(0f, -10f)
                }
            }

            ImGui.popFont()
        }
    }

    override fun value(current: CodeGen.Current): GenValue.Vec2 {
        return if(mode == AttributeMode.INPUT && !hasLink) {
            GenValue.Vec2.Vector2(0.0, 0.0)
        } else {
            value(
                current, "a Vector2"
            ) { it is GenValue.Vec2 }
        }
    }

}