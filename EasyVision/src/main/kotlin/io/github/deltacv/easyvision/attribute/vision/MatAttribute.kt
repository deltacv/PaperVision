package io.github.deltacv.easyvision.attribute.vision

import imgui.ImGui
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.ImageDisplayWindow
import io.github.deltacv.easyvision.gui.style.rgbaColor
import io.github.deltacv.easyvision.gui.util.ExtraWidgets
import io.github.deltacv.easyvision.serialization.data.SerializeData

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    var allowPrevizButton: Boolean = false
) : TypedAttribute(Companion) {

    companion object: Type {
        override val name = "Image"

        override val styleColor = rgbaColor(255, 213, 79, 180)
        override val styleHoveredColor = rgbaColor(255, 213, 79, 255)

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

    @SerializeData
    var isPrevizEnabled = false
        private set
    private var prevIsPrevizEnabled = false

    var wasPrevizJustEnabled = false
        private set

    var displayWindow: ImageDisplayWindow? = null
        private set

    override fun drawAfterText() {
        if(mode == AttributeMode.OUTPUT && allowPrevizButton && isOnEditor) {
            ImGui.sameLine()

            ImGui.pushFont(editor.eyeFont.imfont)
                val text = if (isPrevizEnabled) "-" else "+"

                isPrevizEnabled = ExtraWidgets.toggleButton(
                    text, isPrevizEnabled
                )
            ImGui.popFont()
        }

        val wasButtonToggled = (isPrevizEnabled != prevIsPrevizEnabled)
        wasPrevizJustEnabled = wasButtonToggled && isPrevizEnabled

        if(wasPrevizJustEnabled) {
            displayWindow = editor.startImageDisplayFor(this, "Preview###$id")
        } else if(wasButtonToggled) {
            displayWindow?.delete()
            displayWindow = null
        }

        if(wasButtonToggled) {
            editor.onDraw.doOnce {
                onChange.run()
            }
        }

        prevIsPrevizEnabled = isPrevizEnabled
    }

    override fun value(current: CodeGen.Current) = value<GenValue.Mat>(
        current, "a Mat"
    ) { it is GenValue.Mat }

    fun enablePrevizButton() = apply { allowPrevizButton = true }

    override fun restore() {
        super.restore()

        isPrevizEnabled = false
        prevIsPrevizEnabled = false
        displayWindow = null
    }

}