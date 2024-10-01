package io.github.deltacv.papervision.plugin.gui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.gui.Font
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetInputSourcesMessage
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceData
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceType
import io.github.deltacv.papervision.plugin.ipc.message.SetInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.util.flags

class InputSourceWindow(
    val inputSourcesIconFont: Font,
    val client: PaperVisionEngineClient
) : Window(){
    var inputSources = arrayOf<InputSourceData>()

    private var previousInputSource: String? = null
    var currentInputSource: String? = null
        private set

    override var title = "$[win_inputsources]"
    override val windowFlags = flags(
        ImGuiWindowFlags.AlwaysAutoResize,
    )

    init {
        refreshWithClient()
    }

    fun refreshWithClient() {
        client.sendMessage(GetInputSourcesMessage().onResponseWith<InputSourcesListResponse> {
            inputSources = it.sources

            client.sendMessage(GetCurrentInputSourceMessage().onResponseWith<StringResponse> { currentSourceResponse ->
                currentInputSource = currentSourceResponse.value
            })
        })
    }

    override fun drawContents() {
        if(ImGui.beginListBox("###$id")) {
            for (inputSource in inputSources) {
                ImGui.pushFont(inputSourcesIconFont.imfont)

                val type = when(inputSource.type) {
                    InputSourceType.IMAGE -> "i"
                    InputSourceType.CAMERA -> "c"
                    InputSourceType.VIDEO -> "v"
                }

                ImGui.text("$type-")

                ImGui.popFont()

                ImGui.sameLine()

                if(ImGui.selectable(inputSource.name, currentInputSource == inputSource.name)) {
                    currentInputSource = inputSource.name
                }
            }
            ImGui.endListBox()
        }

        if(previousInputSource != currentInputSource && currentInputSource != null) {
            client.sendMessage(SetInputSourceMessage(currentInputSource!!))
        }

        previousInputSource = currentInputSource
    }
}