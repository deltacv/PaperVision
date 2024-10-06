package io.github.deltacv.papervision.action.editor

import io.github.deltacv.papervision.action.Action
import io.github.deltacv.papervision.node.Node

class CreateNodeAction(
    val node: Node<*>
) : Action() {
    override fun undo() {
        if(node.isEnabled)
            node.delete()
    }

    override fun execute() {
        if(node.isEnabled) return

        if(node.hasEnabled) {
            node.restore()
        } else {
            node.enable()
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