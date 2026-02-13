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

package io.github.deltacv.papervision.plugin

import imgui.app.Application
import io.github.deltacv.papervision.engine.client.response.JsonElementResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.gui.editor.Option
import io.github.deltacv.papervision.gui.TooltipPopup
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.plugin.gui.imgui.CloseConfirmWindow
import io.github.deltacv.papervision.plugin.gui.imgui.InputSourceWindow
import io.github.deltacv.papervision.plugin.ipc.EOCVSimIpcEngineBridge
import io.github.deltacv.papervision.plugin.ipc.message.DiscardCurrentRecoveryMessage
import io.github.deltacv.papervision.plugin.ipc.message.EditorChangeMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentProjectMessage
import io.github.deltacv.papervision.plugin.ipc.message.SaveCurrentProjectMessage
import io.github.deltacv.papervision.serialization.PaperVisionSerializer.deserializeAndApply
import io.github.deltacv.papervision.serialization.PaperVisionSerializer.serializeToTree
import io.github.deltacv.papervision.util.loggerForThis
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

class IpcPaperVisionMain : Callable<Int?> {
    @CommandLine.Option(names = ["-i", "--ipcport"], description = ["Engine IPC server port"])
    var ipcPort: Int = 0

    @CommandLine.Option(names = ["-q", "--queryproject"], description = ["Asks the engine for the current project on startup"])
    var queryProject: Boolean = false

    private lateinit var app: PaperVisionApp

    private var userCloseRequestsCount = 0

    private val logger by loggerForThis()

    override fun call(): Int {
        logger.info("IPC port {}", ipcPort)

        val bridge = EOCVSimIpcEngineBridge(ipcPort)

        app = PaperVisionApp(bridge, false, ::paperVisionUserCloseListener)

        app.paperVision.onUpdate.once {
            if (queryProject) {
                app.paperVision.engineClient.sendMessage(GetCurrentProjectMessage().onResponseWith<JsonElementResponse> { response ->
                    val json = response.value

                    app.paperVision.onUpdate.once {
                        deserializeAndApply(json, app.paperVision)

                        app.paperVision.nodeEditor.onEditorChange {
                            app.paperVision.onUpdate.once {
                                app.paperVision.engineClient.sendMessage(
                                    EditorChangeMessage(
                                        serializeToTree(
                                            app.paperVision.nodes.inmutable,
                                            app.paperVision.links.inmutable
                                        )
                                    )
                                )
                            }
                        }
                    }
                })
            }
        }

        app.paperVision.onInit.once {
            val inputSourceWindow = InputSourceWindow(
                app.paperVision.engineClient
            )

            app.paperVision.nodeEditor.streamWindowGroup.add(inputSourceWindow)

            app.paperVision.nodeEditor.options[FontAwesomeIcons.Save] = Option("mis_saveproject") {
                app.paperVision.engineClient.sendMessage(
                    SaveCurrentProjectMessage(
                        serializeToTree(
                            app.paperVision.nodes.inmutable, app.paperVision.links.inmutable
                        )
                    ).onResponseWith<OkResponse> {
                        app.paperVision.onUpdate.once {
                            TooltipPopup("mis_projectsaved", 4.0).enable()
                        }

                        logger.info("Project saved")
                    }
                )
            }
        }

        Application.launch(app)

        return 0
    }

    private fun paperVisionUserCloseListener(): Boolean {
        userCloseRequestsCount++

        app.paperVision.onUpdate.once {
            openCloseConfirmDialog()
        }

        return userCloseRequestsCount >= 3
    }

    private fun openCloseConfirmDialog() {
        CloseConfirmWindow { action: CloseConfirmWindow.Action ->
            userCloseRequestsCount = 0

            when (action) {
                CloseConfirmWindow.Action.YES -> app.paperVision.engineClient.sendMessage(
                    SaveCurrentProjectMessage(
                        serializeToTree(
                            app.paperVision.nodes.inmutable, app.paperVision.links.inmutable
                        )
                    ).onResponse { response: PaperVisionEngineMessageResponse? ->
                        if (response is OkResponse) {
                            exitProcess(0)
                        }
                    }.onTimeout(2000) {
                        logger.warn("Timeout saving project, exiting anyway")
                        exitProcess(0)
                    }
                )

                CloseConfirmWindow.Action.NO -> app.paperVision.engineClient.sendMessage(
                    DiscardCurrentRecoveryMessage().onResponse { response ->
                        if (response is OkResponse) {
                            exitProcess(0)
                        }
                    }.onTimeout(2000) {
                        logger.warn("Timeout discarding recovery, exiting anyway")
                        exitProcess(0)
                    }
                )

                else -> {}
            }
        }.enable()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(IpcPaperVisionMain()).execute(*args)
            exitProcess(exitCode)
        }
    }
}
