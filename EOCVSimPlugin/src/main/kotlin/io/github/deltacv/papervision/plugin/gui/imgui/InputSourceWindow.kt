package io.github.deltacv.papervision.plugin.gui.imgui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiWindowFlags
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
    val fontAwesomeBig: Font,
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

    private var firstDraw = true

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
            CreateInputSourceWindow(client, fontAwesomeBig).enable()
        }

        if(previousInputSource != currentInputSource && currentInputSource != null) {
            client.sendMessage(SetInputSourceMessage(currentInputSource!!))
        }

        previousInputSource = currentInputSource
    }
}

class CreateInputSourceWindow(
    val client: PaperVisionEngineClient,
    val fontAwesome: Font
) : Window() {
    companion object {
        val separationMultiplier = 1.5f
    }

    override var title = "$[win_createinput_sources]"

    override val windowFlags = flags(
        ImGuiWindowFlags.AlwaysAutoResize,
    )

    override val isModal = true

    override fun drawContents() {
        ImGui.pushStyleColor(ImGuiCol.Button, 0)

        ImGui.pushFont(fontAwesome.imfont)

        if(ImGui.button(FontAwesomeIcons.Image)){
            client.sendMessage(OpenCreateInputSourceMessage(InputSourceType.IMAGE))
            delete()
        }

        ImGui.sameLine()
        ImGui.indent(ImGui.getItemRectSizeX() * separationMultiplier)

        if(ImGui.button(FontAwesomeIcons.Camera)){
            client.sendMessage(OpenCreateInputSourceMessage(InputSourceType.CAMERA))
            delete()
        }

        ImGui.sameLine()
        ImGui.indent(ImGui.getItemRectSizeX() * separationMultiplier)

        if(ImGui.button(FontAwesomeIcons.Video)){
            client.sendMessage(OpenCreateInputSourceMessage(InputSourceType.VIDEO))
            delete()
        }

        ImGui.popFont()
        ImGui.popStyleColor()
    }
}