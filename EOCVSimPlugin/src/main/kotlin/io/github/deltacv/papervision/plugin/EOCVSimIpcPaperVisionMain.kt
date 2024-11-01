/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.plugin

import imgui.app.Application
import io.github.deltacv.papervision.engine.client.response.JsonElementResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.message.OnResponseCallback
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.plugin.gui.imgui.CloseConfirmWindow
import io.github.deltacv.papervision.plugin.gui.imgui.InputSourceWindow
import io.github.deltacv.papervision.plugin.ipc.EOCVSimIpcEngineBridge
import io.github.deltacv.papervision.plugin.ipc.message.DiscardCurrentRecoveryMessage
import io.github.deltacv.papervision.plugin.ipc.message.EditorChangeMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentProjectMessage
import io.github.deltacv.papervision.plugin.ipc.message.SaveCurrentProjectMessage
import io.github.deltacv.papervision.plugin.ipc.stream.JpegStreamClient
import io.github.deltacv.papervision.serialization.PaperVisionSerializer.deserializeAndApply
import io.github.deltacv.papervision.serialization.PaperVisionSerializer.serializeToTree
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

class EOCVSimIpcPaperVisionMain : Callable<Int?> {
    @CommandLine.Option(names = ["-i", "--ipcport"], description = ["Engine IPC server port"])
    var ipcPort: Int = 0

    @CommandLine.Option(names = ["-j", "--jpegport"], description = ["JPEG stream server port"])
    var jpegPort: Int = 0

    @CommandLine.Option(names = ["-q", "--queryproject"], description = ["Asks the engine for the current project"])
    var queryProject: Boolean = false

    private lateinit var app: PaperVisionApp

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun call(): Int {
        logger.info("IPC port {}, JPEG port {}", ipcPort, jpegPort)

        val bridge = EOCVSimIpcEngineBridge(ipcPort)
        val jpegStream = JpegStreamClient(jpegPort, bridge).apply {
            start()
        }

        app = PaperVisionApp(bridge, false, ::paperVisionUserCloseListener)

        app.paperVision.onUpdate.doOnce  {
            if (queryProject) {
                app.paperVision.engineClient.sendMessage(GetCurrentProjectMessage().onResponse { response ->
                    if (response is JsonElementResponse) {
                        val json = response.value

                        app.paperVision.onUpdate.doOnce {
                            deserializeAndApply(json, app.paperVision)

                            app.paperVision.onUpdate.doOnce {
                                app.paperVision.nodeEditor.onEditorChange {
                                    app.paperVision.engineClient.sendMessage(EditorChangeMessage(
                                        serializeToTree(app.paperVision.nodes.inmutable, app.paperVision.links.inmutable)
                                    ))
                                }
                            }
                        }
                    }
                })
            }
        }

        app.paperVision.onInit.doOnce {
            InputSourceWindow(
                app.paperVision.fontAwesome,
                app.paperVision.fontAwesomeBig,
                app.paperVision.engineClient
            ).enable()
        }

        Application.launch(app)

        return 0
    }

    private fun paperVisionUserCloseListener(): Boolean {
        app.paperVision.onUpdate.doOnce {
            openCloseConfirmDialog()
        }

        return false
    }

    private fun openCloseConfirmDialog() {
        CloseConfirmWindow { action: CloseConfirmWindow.Action ->
            when (action) {
                CloseConfirmWindow.Action.YES -> app.paperVision.engineClient.sendMessage(
                    SaveCurrentProjectMessage(
                        serializeToTree(
                            app.paperVision.nodes.inmutable, app.paperVision.links.inmutable
                        )
                    ).onResponse(OnResponseCallback { response: PaperVisionEngineMessageResponse? ->
                        if (response is OkResponse) {
                            exitProcess(0)
                        }
                    })
                )

                CloseConfirmWindow.Action.NO -> app.paperVision.engineClient.sendMessage(
                    DiscardCurrentRecoveryMessage().onResponse { response ->
                        if (response is OkResponse) {
                            exitProcess(0)
                        }
                    }
                )

                else -> {}
            }
        }.enable()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(EOCVSimIpcPaperVisionMain()).execute(*args)
            exitProcess(exitCode)
        }
    }
}