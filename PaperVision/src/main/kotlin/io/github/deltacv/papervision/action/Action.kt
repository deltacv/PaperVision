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