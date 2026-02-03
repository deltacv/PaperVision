/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.gui.editor

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import org.deltacv.mai18n.tr
import io.github.deltacv.papervision.Build
import io.github.deltacv.papervision.gui.util.ARCH
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.gui.util.OS
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.util.getMemoryUsageMB
import io.github.deltacv.papervision.id.container.IdContainerStacks
import io.github.deltacv.papervision.io.resourceToString
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.flags

class AboutModalWindow : Window() {
    override var title = "win_welcome"

    val imguiFont = Font.find("default-12")
    val monoFont = Font.find("jetbrains-mono")

    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize,
        ImGuiWindowFlags.AlwaysAutoResize,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse,
        ImGuiWindowFlags.HorizontalScrollbar
    )

    override val modal = ModalMode.Modal()

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
        for(container in IdContainerStacks.local.all()) {
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
