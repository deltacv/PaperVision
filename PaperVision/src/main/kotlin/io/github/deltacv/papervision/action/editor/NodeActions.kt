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

package io.github.deltacv.papervision.action.editor

import io.github.deltacv.papervision.action.Action
import io.github.deltacv.papervision.node.Node

class CreateNodesAction(
    val nodes: List<Node<*>>
) : Action() {

    constructor(node: Node<*>) : this(listOf(node))

    override fun undo() {
        for(node in nodes) {
            if(node.isEnabled) {
                node.delete()
            }
        }
    }

    override fun execute() {
        for(node in nodes) {
            if(node.isEnabled) continue

            if(node.hasEnabled) {
                node.restore()
            } else {
                node.enable()
            }
        }
    }
}

class DeleteNodesAction(
    val nodes: List<Node<*>>
) : Action() {
    override fun undo() {
        nodes.forEach {
            if(it.isEnabled) return

            if(it.hasEnabled) {
                it.restore()
            } else {
                it.enable()
            }
        }
    }

    override fun execute() {
        nodes.forEach {
            it.delete()
        }
    }
}