package io.github.deltacv.easyvision.gui.util

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.util.ElapsedTime

object PopupBuilder {

    private val tooltips = mutableListOf<ToolTip>()
    private val labels = mutableMapOf<String, Label>()

    fun addWarningToolTip(message: String, w: Float? = null, h: Float? = null) {
        deleteLabel("WARN")
        addToolTip(
            ImGui.getMousePosX(),
            ImGui.getMousePosY(),
            null, null,
            message,
            4.0, label = "WARN"
        )
    }

    fun addToolTip(x: Float, y: Float, w: Float? = null, h: Float? = null,
                   message: String, time: Double? = null, label: String = "") {
        addToolTip(x, y, w, h, time, label) {
            ImGui.text(message)
        }
    }


    fun addToolTip(x: Float, y: Float, w: Float? = null, h: Float? = null,
                   time: Double? = null, label: String = "", drawCallback: () -> Unit) {
        val tooltip = ToolTip(x, y, w, h, time, drawCallback)
        tooltips.add(tooltip)

        labels[label] = Label(tooltip) {
            tooltips.remove(tooltip)
        }
    }

    fun deleteLabel(label: String) {
        labels[label]?.deleteCall?.invoke()
        labels.remove(label)
    }

    fun draw() {
        for(tooltip in tooltips.toTypedArray()) {
            var tooltipLabel: Label? = null
            var tooltipLabelName: String? = null

            for((name, label) in labels) {
                if(label.any == tooltip) {
                    tooltipLabel = label
                    tooltipLabelName = name
                    break
                }
            }

            if((tooltipLabel != null && tooltipLabelName == "WARN" && ImGui.isAnyMouseDown()) // deleting WARN tooltip when mouse is clicked
                || (tooltip.time != null && tooltip.elapsedTime.seconds >= tooltip.time)
            ) {
                tooltips.remove(tooltip)
                tooltipLabel!!.deleteCall()

                continue
            }

            tooltip.draw()
        }
    }

    private data class ToolTip(val x: Float, val y: Float, val w: Float?, val h: Float?,
                               val time: Double?, val callback: () -> Unit) {

        val elapsedTime by lazy { ElapsedTime() }

        fun draw() {
            elapsedTime.seconds

            ImGui.setNextWindowPos(x, y)
            if(w != null && h != null) {
                ImGui.setNextWindowSize(w, h)
            }

            ImGui.beginTooltip()
                callback()
            ImGui.endTooltip()
        }

    }

    private data class Label(val any: Any, val deleteCall: () -> Unit)

}