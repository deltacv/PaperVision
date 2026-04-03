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

package org.deltacv.papervision.gui.display

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import org.deltacv.papervision.attribute.EmptyInputAttribute
import org.deltacv.papervision.attribute.vision.MatAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.NoSession
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v1.data.SerializeIgnore
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_previewdisplay",
    category = NodeCategory.HIGH_LEVEL_CV,
    showInList = false
)
@SerializeIgnore
class ImageDisplayNode(
    val imageDisplay: ImageDisplay
) : DrawNode<NoSession>(joinActionStack = false) {

    val input = EmptyInputAttribute(this)

    override fun drawNode() {
        ImNodes.pushColorStyle(ImNodesCol.Pin, MatAttribute.styleColor)
        ImNodes.pushColorStyle(ImNodesCol.PinHovered, MatAttribute.styleHoveredColor)

        ImNodes.beginInputAttribute(input.id)
        ImNodes.endInputAttribute()

        ImNodes.popColorStyle()
        ImNodes.popColorStyle()

        ImGui.sameLine()

        imageDisplay.draw()
    }

    override fun delete() {
        super.delete()
        input.delete()
    }

    override fun genCode(input: Unit, current: CodeGen.Current) = NoSession

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.ignore()
    }

}



