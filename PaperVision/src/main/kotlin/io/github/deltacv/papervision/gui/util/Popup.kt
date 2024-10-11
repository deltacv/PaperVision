package io.github.deltacv.papervision.gui.util

import imgui.ImGui
import imgui.type.ImBoolean
import io.github.deltacv.papervision.id.DrawableIdElementBase
import io.github.deltacv.papervision.id.IdElementContainerStack

abstract class Popup : DrawableIdElementBase<Popup>() {
    override val idElementContainer = IdElementContainerStack.threadStack.peekNonNull<Popup>()

    abstract val title: String
    abstract val flags: Int

    private var open = false

    var isVisible = false
        private set

    val position = ImGui.getMousePos()
    val idName by lazy { "${title}###$id" }

    private val pOpen = ImBoolean(false)

    abstract fun drawContents()

    override fun draw() {
        if(open) {
            ImGui.openPopup(idName)
            open = false
        }

        if (ImGui.beginPopup(idName, flags)) {
            drawContents()
            ImGui.endPopup()
        }

        isVisible = ImGui.isPopupOpen(idName)
    }

    fun open() {
        if(!isEnabled) {
            enable()
        }

        open = true
    }
}