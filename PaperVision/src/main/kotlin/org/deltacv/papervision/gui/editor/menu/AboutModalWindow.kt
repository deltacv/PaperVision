package org.deltacv.papervision.gui.editor.menu

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import org.deltacv.papervision.Build
import org.deltacv.papervision.gui.Window
import org.deltacv.papervision.gui.util.ARCH
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.gui.util.ImGuiEx
import org.deltacv.papervision.gui.util.OS
import org.deltacv.papervision.gui.util.getMemoryUsageMB
import org.deltacv.papervision.id.container.IdContext
import org.deltacv.papervision.io.resourceToString
import org.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr

class AboutModalWindow : Window() {
    override var title = "win_welcome"

    val imguiFont by Font.findLazy("default-12")
    val monoFont by Font.findLazy("jetbrains-mono-big")

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.HorizontalScrollbar
    )

    override val modal = ModalMode.Modal()

    private var isFirstDraw = true

    companion object {
        val icon = resourceToString("/ico/ico_ezv.txt")
        val iconLogo = resourceToString("/ico/ico_ezv_logo.txt")
    }

    override fun drawContents() {

        ImGui.newLine()
        ImGui.newLine()

        imguiFont.push()
        ImGuiEx.centeredText(icon)
        ImGui.popFont()

        ImGui.newLine()
        ImGui.newLine()

        monoFont.push()

        ImGuiEx.centeredText("PaperVision v${Build.VERSION_STRING} built on ${Build.BUILD_DATE}")

        if(Build.IS_DEV) {
            ImGuiEx.centeredText("You are running a development build. Report any issues to the developers.")
        } else {
            ImGuiEx.centeredText("You are running a stable build.")
        }

        ImGui.newLine()

        val OS_VERSION = System.getProperty("os.version")

        var elementCount = 0
        for(container in IdContext.Companion.local.all()) {
            elementCount += container.inmutable.size
        }

        ImGuiEx.centeredText("System details: ${OS} $OS_VERSION ${ARCH} running on Java ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}")
        ImGuiEx.centeredText("Current heap memory usage: ${getMemoryUsageMB()} MB | Element count: $elementCount")

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

    private fun alignForWidth(width: Float, alignment: Float): Float {
        val windowSize = ImGui.getWindowSize()
        val pos = windowSize.x / 2 - width / 2
        ImGui.sameLine(pos + alignment)

        return pos + alignment
    }
}



