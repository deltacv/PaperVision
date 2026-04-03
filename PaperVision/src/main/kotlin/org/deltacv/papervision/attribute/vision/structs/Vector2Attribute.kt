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

package org.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import imgui.ImVec2
import org.deltacv.papervision.action.editor.CreateLinkAction
import org.deltacv.papervision.attribute.AttributeMode
import org.deltacv.papervision.attribute.AttributeType
import org.deltacv.papervision.attribute.TypedAttribute
import org.deltacv.papervision.attribute.decomp.vision.structs.Vector2AttributeDecomposer
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.node.Link
import org.deltacv.papervision.node.math.Vector2Node
import org.deltacv.papervision.serialization.v2.CodecType

@CodecType(instantiable = false)
class Vector2Attribute (
    override val mode: AttributeMode,
    override var variableName: String? = null,
    val useSizeNaming: Boolean = false
) : TypedAttribute<GenValue.Vec2>(Companion) {

    companion object : AttributeType<Vector2Attribute> {
        override val icon = FontAwesomeIcons.DotCircle

        override fun new(mode: AttributeMode, variableName: String) = Vector2Attribute(mode, variableName)

        override fun newDecomposer() = Vector2AttributeDecomposer()
    }

    private val fontAwesome = Font.find("font-awesome")

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushFont(fontAwesome.imfont)

            if(!hasLink && ImGui.button(FontAwesomeIcons.PencilAlt)) {
                val node = parentNode.editor.addNode(Vector2Node(useSizeNaming))

                parentNode.editor.onDraw.once {
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

    override fun genValue(current: CodeGen.Current) =
        readGenValue<GenValue.Vec2>(current, GenValue.Vec2.Actual(GenValue.Int.ZERO, GenValue.Int.ZERO))

}



