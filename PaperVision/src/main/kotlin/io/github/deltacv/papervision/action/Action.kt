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
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.util.loggerForThis

abstract class Action(
    val executeOnEnable: Boolean = true
) : DrawableIdElementBase<Action>() {
    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Action>()

    val logger by loggerForThis()

    override fun enable() {
        if(!idElementContainer.stackPointerFollowing) {
            logger.info("Forking after pointer")
            idElementContainer.fork()
        }
        super.enable()
    }

    override fun onEnable() {
        if(executeOnEnable) {
            execute()
        }
    }

    override fun draw() {}

    abstract fun undo()
    abstract fun execute()
}

class RootAction : Action(
    executeOnEnable = false
) {
    override fun undo() {}

    override fun execute() {
        logger.info("Root action executed")
        idElementContainer.pushforwardIfNonNull()
    }
}