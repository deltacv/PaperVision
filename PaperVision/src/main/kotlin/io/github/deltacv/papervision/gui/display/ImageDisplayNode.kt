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

package io.github.deltacv.papervision.gui.display

import imgui.ImGui
import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.attribute.EmptyInputAttribute
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

@PaperNode(
    name = "nod_previewdisplay",
    category = Category.HIGH_LEVEL_CV,
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

    override fun genCode(current: CodeGen.Current) = NoSession

}
