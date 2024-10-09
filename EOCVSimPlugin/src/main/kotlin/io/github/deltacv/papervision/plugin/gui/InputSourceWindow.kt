package io.github.deltacv.papervision.plugin.gui

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.gui.Font
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.gui.util.Window
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetInputSourcesMessage
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceData
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceType
import io.github.deltacv.papervision.plugin.ipc.message.OpenCreateInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.SetInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.util.flags

class InputSourceWindow(
    val fontAwesome: Font,
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
                ImGui.pushFont(fontAwesome.imfont)

                val type = when(inputSource.type) {
                    InputSourceType.IMAGE -> FontAwesomeIcons.Image
                    InputSourceType.CAMERA -> FontAwesomeIcons.Camera
                    InputSourceType.VIDEO -> FontAwesomeIcons.Video
                }

                ImGui.text(type)

                ImGui.popFont()

                ImGui.sameLine()

                if(ImGui.selectable(inputSource.name, currentInputSource == inputSource.name)) {
                    currentInputSource = inputSource.name
                }
            }
            ImGui.endListBox()
        }

        if(ImGui.button("Create new input source")) {
            CreateInputSourceWindow(client).enable()
        }

        if(previousInputSource != currentInputSource && currentInputSource != null) {
            client.sendMessage(SetInputSourceMessage(currentInputSource!!))
        }

        previousInputSource = currentInputSource
    }
}

class CreateInputSourceWindow(
    val client: PaperVisionEngineClient
) : Window() {
    override var title = "$[win_createinputsources]"

    override val windowFlags = flags(
        ImGuiWindowFlags.AlwaysAutoResize,
    )

    override val isModal = true

    val inputIndex = ImInt()

    override fun drawContents() {
        ImGui.combo("###$id", inputIndex, arrayOf("Image", "Camera", "Video"), 3)

        if(ImGui.button("Create")) {
            client.sendMessage(OpenCreateInputSourceMessage(
                when(inputIndex.get()) {
                    1 -> InputSourceType.CAMERA
                    2 -> InputSourceType.VIDEO
                    else -> InputSourceType.IMAGE
                }
            ))

            delete()
        }
    }
}