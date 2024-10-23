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
import io.github.deltacv.papervision.node.Link

class CreateLinkAction(
    val link: Link
) : Action() {
    override fun undo() {
        link.delete()
    }

    override fun execute() {
        if(link.isEnabled) return

        if(link.hasEnabled) {
            link.restore()
        } else {
            link.enable()
        }
    }
}

class DeleteLinksAction(
    val links: List<Link>
) : Action() {
    override fun undo() {
        links.forEach {
            if(it.isEnabled) return


            it.enable()
        }
    }

    override fun execute() {
        links.forEach {
            it.delete()
        }
    }
}