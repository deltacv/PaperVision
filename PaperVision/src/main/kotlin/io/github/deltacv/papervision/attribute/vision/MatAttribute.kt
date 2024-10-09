package io.github.deltacv.papervision.attribute.vision

import imgui.ImGui
import imgui.flag.ImGuiCol
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.eocvsim.ImageDisplayNode
import io.github.deltacv.papervision.gui.style.rgbaColor
import io.github.deltacv.papervision.gui.util.ExtraWidgets
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

class MatAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null,
    var allowPrevizButton: Boolean = false
) : TypedAttribute(Companion) {

    companion object: AttributeType {
        override val icon = FontAwesomeIcons.Image

        override val styleColor = rgbaColor(255, 213, 79, 180)
        override val styleHoveredColor = rgbaColor(255, 213, 79, 255)

        override fun new(mode: AttributeMode, variableName: String) = MatAttribute(mode, variableName)
    }

    @SerializeIgnore
    var isPrevizEnabled = false
        private set

    private var prevIsPrevizEnabled = false

    @SerializeIgnore
    var wasPrevizJustEnabled = false
        private set

    @SerializeIgnore
    var displayWindow: ImageDisplayNode? = null
        private set

    override fun drawAfterText() {
        if(mode == AttributeMode.OUTPUT && allowPrevizButton && isOnEditor) {
            ImGui.sameLine()

            ImGui.pushFont(editor.fontAwesome.imfont)
                val text = if (isPrevizEnabled) FontAwesomeIcons.EyeSlash else FontAwesomeIcons.Eye

                ImGui.pushStyleColor(ImGuiCol.Button, 0)

                isPrevizEnabled = ExtraWidgets.toggleButton(
                    text, isPrevizEnabled
                )

                ImGui.popStyleColor()
            ImGui.popFont()
        }

        val wasButtonToggled = (isPrevizEnabled != prevIsPrevizEnabled)
        wasPrevizJustEnabled = wasButtonToggled && isPrevizEnabled

        if(wasPrevizJustEnabled) {
            displayWindow = editor.startImageDisplayFor(this)

            displayWindow!!.onDelete.doOnce {
                isPrevizEnabled = false
                displayWindow = null
            }
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