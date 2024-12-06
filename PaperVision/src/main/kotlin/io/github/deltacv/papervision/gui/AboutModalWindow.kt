package io.github.deltacv.papervision.gui

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.mai18n.tr
import io.github.deltacv.papervision.Build
import io.github.deltacv.papervision.gui.util.ARCH
import io.github.deltacv.papervision.gui.util.OS
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.gui.util.getMemoryUsageMB
import io.github.deltacv.papervision.gui.util.getProcessCPULoad
import io.github.deltacv.papervision.id.IdElementContainerStack
import io.github.deltacv.papervision.io.resourceToString
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags

class AboutModalWindow(
    val imguiFont: Font,
    val monoFont: Font
) : Window() {
    override var title = "win_welcome"

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.HorizontalScrollbar
    )

    override val isModal = true

    val onDontShowAgain = PaperVisionEventHandler("IntroModalWindow-OnDontShowAgain")

    private var isFirstDraw = true

    companion object {
        val icon = resourceToString("/ico/ico_ezv.txt")
        val iconLogo = resourceToString("/ico/ico_ezv_logo.txt")
    }

    override fun drawContents() {

        ImGui.newLine()
        ImGui.newLine()

        ImGui.pushFont(imguiFont.imfont)
        centeredText(icon)
        ImGui.popFont()

        ImGui.newLine()
        ImGui.newLine()

        ImGui.pushFont(monoFont.imfont)

        centeredText("PaperVision v${Build.VERSION_STRING} built on ${Build.BUILD_DATE}")

        if(Build.IS_DEV) {
            centeredText("You are running a development build. Report any issues to the developers.")
        } else {
            centeredText("You are running a stable build.")
        }

        ImGui.newLine()

        val OS_VERSION = System.getProperty("os.version")

        var elementCount = 0
        for(container in IdElementContainerStack.threadStack.all()) {
            elementCount += container.elements.size
        }

        centeredText("System details: $OS $OS_VERSION $ARCH running on Java ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}")
        centeredText("Current heap memory usage: ${getMemoryUsageMB()} MB | Element count: $elementCount")

        ImGui.newLine()

        var width = ImGui.calcTextSize(tr("mis_gotit")).x
        alignForWidth(width, 0.5f)

        if (ImGui.button(tr("mis_gotit"))) {
            delete()
        }

        ImGui.popFont()

        ImGui.sameLine()

        centerWindow()

        if (!isFirstDraw) {
            size = ImVec2(ImGui.getMainViewport().sizeX * 0.6f, ImGui.getWindowSizeY())
        }

        isFirstDraw = false
    }

    private fun centeredText(text: String) {
        val textSize = ImGui.calcTextSize(tr(text))
        val windowSize = ImGui.getWindowSize()
        val pos = windowSize.x / 2 - textSize.x / 2
        ImGui.sameLine(pos)
        ImGui.text(tr(text))
        ImGui.newLine()
    }

    private fun alignForWidth(width: Float, alignment: Float): Float {
        val windowSize = ImGui.getWindowSize()
        val pos = windowSize.x / 2 - width / 2
        ImGui.sameLine(pos + alignment)

        return pos + alignment
    }
}