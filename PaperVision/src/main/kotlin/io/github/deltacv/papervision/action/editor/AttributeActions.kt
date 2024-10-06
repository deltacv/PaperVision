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