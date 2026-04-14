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
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.node.Link
import org.deltacv.papervision.node.vision.overlay.LineParametersNode
import org.deltacv.papervision.serialization.v2.CodecType

@CodecType(instantiable = false)
class LineParametersAttribute(
    override val mode: AttributeMode,
    override var attributeName: String? = null
) : TypedAttribute<GenValue.LineParameters>(Companion) {

    companion object : AttributeType<LineParametersAttribute> {
        override val icon = FontAwesomeIcons.ChartLine

        override fun new(mode: AttributeMode, variableName: String) = LineParametersAttribute(mode, variableName)
    }

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushFont(fontAwesome.imfont)

            if(!hasLink && ImGui.button(FontAwesomeIcons.PencilAlt)) {
                val node = parentNode.editor.addNode(LineParametersNode::class)

                parentNode.editor.onDraw.once {
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

    override fun genValue(current: CodeGen.Current) = readGenValue<GenValue.LineParameters>(
        current, GenValue.LineParameters.Actual(
            GenValue.Scalar.Components(GenValue.Double.ZERO, GenValue.Double.Actual(255.0.resolved()), GenValue.Double.ZERO, GenValue.Double.ZERO),
            GenValue.Int.Actual(3.resolved())
        )
    )

}



