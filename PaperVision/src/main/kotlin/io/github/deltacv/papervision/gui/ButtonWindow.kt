package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.PaperEventHandler
import io.github.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr

open class ButtonWindow(
    var buttonText: String,
    var buttonFont: Font?,
    var buttonTooltip: String? = null,
    var buttonTooltipFont: Font? = null,
    val hoveringTimeForTooltipSecs: Double = 0.5
) : Window() {

    override var title = "button"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.AlwaysAutoResize
    )

    var hovered = false
        private set
    private var lastPressed = false

    private val hoveringTime = ElapsedTime()

    var isPressed = false
        private set

    val onClick by lazy { PaperEventHandler("ButtonWindow-OnClick") }

    /**
     * Runs before window draw.
     * We only set background color here.
     */
    override fun preDrawContents() {
        val style = ImGui.getStyle()

        val color = when {
            isPressed -> style.getColor(ImGuiCol.ButtonActive)
            hovered -> style.getColor(ImGuiCol.ButtonHovered)
            else -> style.getColor(ImGuiCol.Button)
        }

        ImGui.pushStyleColor(ImGuiCol.WindowBg, color)
        // Remove window padding so button fills entire window
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
    }

    /**
     * Actual rendering + input handling
     */
    override fun drawContents() {
        // ---- interactive region ----
        ImGui.invisibleButton("##$id", size.x.coerceAtLeast(1f), size.y.coerceAtLeast(1f))

        hovered = ImGui.isItemHovered()
        val held = ImGui.isItemActive()

        if(hovered) {
            buttonTooltip?.let { text ->
                if(hoveringTime.seconds >= hoveringTimeForTooltipSecs) {
                    buttonTooltipFont?.imfont?.let { ImGui.pushFont(it) }

                    ImGui.beginTooltip()
                    ImGui.text(tr(text))
                    ImGui.endTooltip()

                    buttonTooltipFont?.imfont?.let { ImGui.popFont() }
                }
            }
        } else {
            hoveringTime.reset()
        }

        // Check if button was released while hovered (proper button behavior)
        val released = lastPressed && !held && hovered

        isPressed = held

        if (released) {
            onClick.run()
        }
        lastPressed = held

        // ---- centered text ----
        buttonFont?.imfont?.let { ImGui.pushFont(it) }

        val textSize = ImGui.calcTextSize(buttonText)

        ImGui.setCursorPos(
            (size.x - textSize.x) / 2f,
            (size.y - textSize.y) / 2f
        )

        ImGui.text(tr(buttonText))

        buttonFont?.imfont?.let { ImGui.popFont() }

        // ---- restore style ----
        ImGui.popStyleVar() // WindowPadding
        ImGui.popStyleColor() // WindowBg
    }
}