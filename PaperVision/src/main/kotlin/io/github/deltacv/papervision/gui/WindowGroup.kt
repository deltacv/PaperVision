package io.github.deltacv.papervision.gui

import imgui.ImVec2
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.util.flags

enum class LayoutDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP
}

sealed class SizingMode {
    /**
     * Windows keep their original size
     */
    object None : SizingMode()

    /**
     * All windows get equal size based on available space
     * For horizontal layout: equal widths, full height
     * For vertical layout: full width, equal heights
     */
    object Grid : SizingMode()

    /**
     * All windows get the specified fixed size, and the WindowGroup size is calculated
     * For horizontal layout: itemSize.x per window, itemSize.y for height
     * For vertical layout: itemSize.x for width, itemSize.y per window
     */
    data class GridFixed(val itemSizeProvider: () -> ImVec2) : SizingMode() {
        constructor(itemSize: ImVec2) : this({ itemSize })
    }

    /**
     * All windows get the size of the largest window (dynamically updated each frame)
     * For horizontal layout: largest width for all, largest height for all
     * For vertical layout: largest width for all, largest height for all
     * WindowGroup size is calculated based on this largest size
     */
    object GridDynamic : SizingMode()

    /**
     * Custom sizing logic (extensible for future use)
     */
    data class Custom(val sizer: (Window, Int, Int, ImVec2) -> ImVec2) : SizingMode()
}

class WindowGroup(
    vararg windows: Window,
    var direction: LayoutDirection = LayoutDirection.LEFT_TO_RIGHT,
    var spacing: Float = 10f,
    var padding: Float = 0f,  // outer offset from group origin
    var sizingMode: SizingMode = SizingMode.None
) : Window() {

    private val orderedWindows = mutableListOf<Window>()
    private val initialWindows = windows.toList()

    override var title = "Window Group"
    override val windowFlags = flags(
        ImGuiWindowFlags.NoBackground,
        ImGuiWindowFlags.NoDecoration,
        ImGuiWindowFlags.NoMove,
        ImGuiWindowFlags.NoResize
    )

    override fun onEnable() {
        for(window in initialWindows) {
            add(window)
        }
    }

    /**
     * Add a window to the group (will be positioned according to layout direction)
     */
    fun add(vararg windows: Window) {
        for(window in windows) {
            if(window.isEnabled) {
                throw IllegalStateException("Cannot add already enabled window '${window.title}' to WindowGroup")
            }

            window.enable()
            orderedWindows.add(window)
        }
    }

    override fun drawContents() {
        // Empty - children draw themselves
    }

    override fun postDrawContents() {
        if (orderedWindows.isEmpty()) return

        // Get WindowGroup's position as the origin for the FIRST element
        val groupPos = position
        var groupSize = size

        // For GridFixed and GridDynamic modes, calculate the group size
        when (sizingMode) {
            is SizingMode.GridFixed -> {
                groupSize = calculateGridFixedSize(sizingMode as SizingMode.GridFixed)
                size = groupSize // Update the actual WindowGroup size
            }
            is SizingMode.GridDynamic -> {
                groupSize = calculateGridDynamicSize()
                size = groupSize // Update the actual WindowGroup size
            }
            else -> {}
        }

        // Calculate available space for windows
        val availableWidth = groupSize.x - (padding * 2)
        val availableHeight = groupSize.y - (padding * 2)

        // Apply sizing based on mode
        when (sizingMode) {
            is SizingMode.None -> {
                // Keep original sizes
            }
            is SizingMode.Grid -> {
                applyGridSizing(availableWidth, availableHeight)
            }
            is SizingMode.GridFixed -> {
                applyGridFixedSizing(sizingMode as SizingMode.GridFixed)
            }
            is SizingMode.GridDynamic -> {
                applyGridDynamicSizing()
            }
            is SizingMode.Custom -> {
                applyCustomSizing(availableWidth, availableHeight, sizingMode as SizingMode.Custom)
            }
        }

        // Calculate starting positions based on direction
        // groupPos is the origin of the FIRST element (accounting for direction)
        var currentX = when (direction) {
            LayoutDirection.LEFT_TO_RIGHT -> groupPos.x + padding
            LayoutDirection.RIGHT_TO_LEFT -> groupPos.x - padding  // First element's top-right
            else -> groupPos.x + padding
        }

        var currentY = when (direction) {
            LayoutDirection.TOP_TO_BOTTOM -> groupPos.y + padding
            LayoutDirection.BOTTOM_TO_TOP -> groupPos.y - padding  // First element's bottom
            else -> groupPos.y + padding
        }

        // Position each window
        for (window in orderedWindows) {
            val windowSize = window.size

            // Place window based on direction
            val windowX = when (direction) {
                LayoutDirection.RIGHT_TO_LEFT -> currentX - windowSize.x
                else -> currentX
            }

            val windowY = when (direction) {
                LayoutDirection.BOTTOM_TO_TOP -> currentY - windowSize.y
                else -> currentY
            }

            window.position = ImVec2(windowX, windowY)

            // Advance position for next window
            when (direction) {
                LayoutDirection.LEFT_TO_RIGHT -> currentX += windowSize.x + spacing
                LayoutDirection.RIGHT_TO_LEFT -> currentX -= windowSize.x + spacing
                LayoutDirection.TOP_TO_BOTTOM -> currentY += windowSize.y + spacing
                LayoutDirection.BOTTOM_TO_TOP -> currentY -= windowSize.y + spacing
            }
        }
    }

    private fun calculateGridDynamicSize(): ImVec2 {
        val count = orderedWindows.size
        if (count == 0) return ImVec2(0f, 0f)

        // Find the largest width and height among all windows
        var maxWidth = 0f
        var maxHeight = 0f

        for (window in orderedWindows) {
            val windowSize = window.size
            if (windowSize.x > maxWidth) maxWidth = windowSize.x
            if (windowSize.y > maxHeight) maxHeight = windowSize.y
        }

        return when (direction) {
            LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.RIGHT_TO_LEFT -> {
                // Width = sum of largest widths + spacing + padding
                val totalWidth = (maxWidth * count) + (spacing * (count - 1)) + (padding * 2)
                // Height = largest height + padding
                val totalHeight = maxHeight + (padding * 2)
                ImVec2(totalWidth, totalHeight)
            }
            LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
                // Width = largest width + padding
                val totalWidth = maxWidth + (padding * 2)
                // Height = sum of largest heights + spacing + padding
                val totalHeight = (maxHeight * count) + (spacing * (count - 1)) + (padding * 2)
                ImVec2(totalWidth, totalHeight)
            }
        }
    }

    private fun applyGridDynamicSizing() {
        // Find the largest width and height among all windows
        var maxWidth = 0f
        var maxHeight = 0f

        for (window in orderedWindows) {
            val windowSize = window.size
            if (windowSize.x > maxWidth) maxWidth = windowSize.x
            if (windowSize.y > maxHeight) maxHeight = windowSize.y
        }

        // Set all windows to the largest size
        for (window in orderedWindows) {
            window.size = ImVec2(maxWidth, maxHeight)
        }
    }

    private fun calculateGridFixedSize(fixedMode: SizingMode.GridFixed): ImVec2 {
        val count = orderedWindows.size
        if (count == 0) return ImVec2(0f, 0f)

        val itemSize = fixedMode.itemSizeProvider()

        return when (direction) {
            LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.RIGHT_TO_LEFT -> {
                // Width = sum of all item widths + spacing + padding
                val totalWidth = (itemSize.x * count) + (spacing * (count - 1)) + (padding * 2)
                // Height = item height + padding
                val totalHeight = itemSize.y + (padding * 2)
                ImVec2(totalWidth, totalHeight)
            }
            LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
                // Width = item width + padding
                val totalWidth = itemSize.x + (padding * 2)
                // Height = sum of all item heights + spacing + padding
                val totalHeight = (itemSize.y * count) + (spacing * (count - 1)) + (padding * 2)
                ImVec2(totalWidth, totalHeight)
            }
        }
    }

    private fun applyGridSizing(availableWidth: Float, availableHeight: Float) {
        val count = orderedWindows.size
        if (count == 0) return

        when (direction) {
            LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.RIGHT_TO_LEFT -> {
                // Equal widths, full height
                val totalSpacing = spacing * (count - 1)
                val widthPerWindow = (availableWidth - totalSpacing) / count

                for (window in orderedWindows) {
                    window.size = ImVec2(widthPerWindow, availableHeight)
                }
            }
            LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
                // Full width, equal heights
                val totalSpacing = spacing * (count - 1)
                val heightPerWindow = (availableHeight - totalSpacing) / count

                for (window in orderedWindows) {
                    window.size = ImVec2(availableWidth, heightPerWindow)
                }
            }
        }
    }

    private fun applyGridFixedSizing(fixedMode: SizingMode.GridFixed) {
        val itemSize = fixedMode.itemSizeProvider()

        // Set all windows to the fixed item size
        for (window in orderedWindows) {
            window.size = ImVec2(itemSize.x, itemSize.y)
        }
    }

    private fun applyCustomSizing(
        availableWidth: Float,
        availableHeight: Float,
        customMode: SizingMode.Custom
    ) {
        val availableSpace = ImVec2(availableWidth, availableHeight)

        orderedWindows.forEachIndexed { index, window ->
            window.size = customMode.sizer(window, index, orderedWindows.size, availableSpace)
        }
    }

    override fun delete() {
        for(window in orderedWindows) {
            window.delete()
        }

        super.delete()
    }

}