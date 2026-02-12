package io.github.deltacv.papervision.gui.editor

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.gui.Table
import io.github.deltacv.papervision.gui.Window
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.io.KeyManager
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.util.flags
import org.deltacv.mai18n.tr

class Headers(
    val keyManager: KeyManager,
    val nodesSupplier: () -> Map<Category, List<Node<*>>>
) : Window() {

    override var title = "headers"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoResize, ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoCollapse, ImGuiWindowFlags.NoTitleBar,
        ImGuiWindowFlags.NoDecoration, ImGuiWindowFlags.AlwaysVerticalScrollbar
    )

    val categoryTables = mutableMapOf<Category, Table>()
    val categoryStates = mutableMapOf<Category, Boolean>()

    var currentScroll = 0f
    var nextScroll: Float? = null

    // Delayed scroll application to work around ImGui's one-frame delay
    private var pendingScroll: Float? = null

    var isHeaderHovered = false
        private set

    val keys = keyManager.keys

    val headerFont = Font.find("calcutta-big")

    override fun preDrawContents() {
        ImGui.setNextWindowPos(0f, 0f)
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0.0f) // transparent headers window
    }

    override fun drawContents() {
        // Handle pending scroll from previous frame
        if (pendingScroll != null) {
            ImGui.setScrollY(pendingScroll!!.coerceIn(0f, ImGui.getScrollMaxY()))
            pendingScroll = null
        }

        val scrollValue = when {
            keyManager.pressing(keys.ArrowUp) -> -0.8f
            keyManager.pressing(keys.ArrowDown) -> 0.8f
            else -> -ImGui.getIO().mouseWheel
        }

        ImGui.setCursorPos(0f, 0f) // draw the node editor on top of the collapsing headers

        isHeaderHovered = false

        for (category in Category.entries) {
            if (nodesSupplier().containsKey(category)) {
                if (!categoryTables.containsKey(category)) {
                    categoryTables[category] = Table()
                }

                ImGui.pushStyleColor(ImGuiCol.Header, category.color)
                ImGui.pushStyleColor(ImGuiCol.HeaderActive, category.colorSelected)
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, category.colorSelected)

                ImGui.pushFont(headerFont.imfont)

                val isOpen = ImGui.collapsingHeader(
                    tr(category.properName), ImGuiTreeNodeFlags.DefaultOpen
                )

                ImGui.popFont()

                categoryStates[category] = isOpen

                ImGui.popStyleColor(3)

                if (ImGui.isItemHovered()) {
                    isHeaderHovered = true
                }

                if (isOpen) {
                    val table = categoryTables[category]!!
                    ImGui.newLine()
                    ImGui.indent(10f)

                    ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 20f, 20f)

                    table.draw()

                    ImGui.popStyleVar()

                    ImGui.newLine()
                    ImGui.unindent(10f)
                }
            }
        }

        // Handle scrolling
        if (nextScroll != null) {
            // Queue the scroll for next frame to avoid one-frame delay issues
            pendingScroll = nextScroll
            nextScroll = null
        } else if (scrollValue != 0f) {
            // User is actively scrolling
            val newScroll = ImGui.getScrollY() + scrollValue * 20.0f
            ImGui.setScrollY(newScroll.coerceIn(0f, ImGui.getScrollMaxY()))
        }

        // Always sync currentScroll to reflect reality
        currentScroll = ImGui.getScrollY()

        ImGui.popStyleColor()
    }

}