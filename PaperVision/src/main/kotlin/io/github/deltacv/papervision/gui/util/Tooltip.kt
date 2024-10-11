package io.github.deltacv.papervision.gui.util

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.id.IdElementContainerStack

open class Tooltip(
    val text: String,
    val position: ImVec2,
    val timeSecs: Double,
    val label: String? = null,
    override val requestedId: Int? = null
) : DrawableIdElementBase<Tooltip>() {

    private val timer = ElapsedTime()

    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Tooltip>()

    override fun onEnable() {
        timer.reset()
    }

    override fun draw() {
        ImGui.setNextWindowPos(position.x, position.y)

        ImGui.beginTooltip()
            drawContents()
        ImGui.endTooltip()

        if(timer.seconds > timeSecs)
            delete()
    }

    open fun drawContents() {
        ImGui.text(tr(text))
    }

    companion object {
        val WARN = 0

        fun warning(text: String, secsPerCharacter: Double = 0.16) {
            Tooltip(text, ImGui.getMousePos(), text.length * secsPerCharacter, requestedId = WARN).enable()
        }
    }

}