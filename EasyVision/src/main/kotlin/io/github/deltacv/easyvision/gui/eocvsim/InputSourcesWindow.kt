package io.github.deltacv.easyvision.gui.eocvsim

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.gui.FontManager
import io.github.deltacv.easyvision.gui.util.Window
import io.github.deltacv.easyvision.util.IpcClientWatchDog
import io.github.deltacv.eocvsim.input.InputSourceData
import io.github.deltacv.eocvsim.input.InputSourceType
import io.github.deltacv.eocvsim.ipc.message.response.IpcStringResponse
import io.github.deltacv.eocvsim.ipc.message.response.sim.InputSourcesListResponse
import io.github.deltacv.eocvsim.ipc.message.sim.GetCurrentInputSourceMessage
import io.github.deltacv.eocvsim.ipc.message.sim.InputSourcesListMessage
import io.github.deltacv.eocvsim.ipc.message.sim.SetInputSourceMessage

class InputSourcesWindow(
    fontManager: FontManager
) : Window() {

    var inputSources: Array<InputSourceData> = arrayOf()

    private var previousInputSource: String? = null
    var currentInputSource: String? = null
        private set

    private var attachedIpc: IpcClientWatchDog? = null
        private set

    override var title = "$[win_inputsources]"
    override val windowFlags = ImGuiWindowFlags.AlwaysAutoResize

    val sourcesFont = fontManager.makeFont("/fonts/icons/Input-Sources.ttf", 13f)

    override fun drawContents() {
        if(ImGui.beginListBox("###$id")) {
            for (inputSource in inputSources) {

                ImGui.pushFont(sourcesFont.imfont)

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
            attachedIpc?.broadcast(SetInputSourceMessage(currentInputSource!!))
        }

        previousInputSource = currentInputSource
    }

    private fun updateWithIpc(client: IpcClientWatchDog) {
        client.broadcast(InputSourcesListMessage().onResponseWith<InputSourcesListResponse> { sourcesResponse ->
            inputSources = sourcesResponse.sources

            client.broadcast(GetCurrentInputSourceMessage().onResponseWith<IpcStringResponse> { currentSourceResponse ->
                currentInputSource = currentSourceResponse.value
            })
        })
    }

    fun attachToIpc(client: IpcClientWatchDog) {
        updateWithIpc(client)

        client.onConnect {
            if(attachedIpc != client) {
                it.removeThis()
            } else updateWithIpc(client)
        }

        attachedIpc = client
    }

}