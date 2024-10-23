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

package io.github.deltacv.papervision.node

import imgui.extension.imnodes.ImNodes
import imgui.extension.imnodes.flag.ImNodesCol
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

@SerializeIgnore
class InvisibleNode : Node<NoSession>(allowDelete = false) {

    private val invisibleColor = rgbaColor(0, 0, 0, 0)

    override fun draw() {
        ImNodes.pushColorStyle(ImNodesCol.NodeOutline, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBar, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.TitleBarSelected, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackground, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundHovered, invisibleColor)
        ImNodes.pushColorStyle(ImNodesCol.NodeBackgroundSelected, invisibleColor)

        ImNodes.beginNode(id)
        ImNodes.endNode()

        repeat(7) {
            ImNodes.popColorStyle()
        }
    }

    override fun genCode(current: CodeGen.Current) = NoSession

}