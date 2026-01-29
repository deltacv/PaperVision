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

package io.github.deltacv.papervision.action

import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdContainerStacks
import io.github.deltacv.papervision.id.StatedIdElementBase
import io.github.deltacv.papervision.util.loggerForThis

abstract class Action(
    val executeOnEnable: Boolean = true
) : StatedIdElementBase<Action>() {
    override val idContainer get() = IdContainerStacks.local.peekNonNull<Action>()

    val logger by loggerForThis()

    override fun enable() {
        if(!idContainer.stackPointerFollowing) {
            logger.info("Forking after pointer")
            idContainer.fork()
        }
        super.enable()
    }

    override fun onEnable() {
        if(executeOnEnable) {
            execute()
        }
    }

    abstract fun undo()
    abstract fun execute()
}

class RootAction : Action(
    executeOnEnable = false
) {
    override fun undo() {}

    override fun execute() {
        logger.debug("Root action reached, nothing to do")
        idContainer.pushforwardIfNonNull()
    }
}
