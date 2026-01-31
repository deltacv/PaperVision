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

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.InputSourceApi
import io.github.deltacv.eocvsim.plugin.api.PipelineManagerApi
import io.github.deltacv.eocvsim.plugin.api.TunableFieldApi
import io.github.deltacv.eocvsim.plugin.loader.FilePluginLoader
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import io.github.deltacv.papervision.engine.client.message.*
import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.client.response.PrevizStatisticsResponse
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.plugin.previz.PaperVisionDefaultPipeline
import io.github.deltacv.papervision.plugin.gui.eocvsim.PaperVisionTabPanel
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.previz.EOCVSimEngineImageStreamer
import io.github.deltacv.papervision.plugin.previz.EOCVSimPrevizSession
import io.github.deltacv.papervision.plugin.ipc.message.*
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.util.replaceLast
import io.github.deltacv.papervision.util.toValidIdentifier
import org.opencv.core.Size
import java.io.File
import java.util.*
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Main entry point for the PaperVision plugin.
 * Specified in the plugin.toml file.
 */
class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val logger by loggerForThis()

    val engine = PaperVisionProcessRunner.paperVisionEngine

    var isRunningPreviewPipeline = false

    var currentPrevizSession: EOCVSimPrevizSession? = null

    private val tunableFieldCache = WeakHashMap<VirtualField, TunableFieldApi>()

    /**
     * If the plugin comes from a file, we will just use the file classpath, since it's a single fat jar.
     * If the plugin comes from Maven, we will use the classpath of all the transitive dependencies.
     */
    val fullClasspath by lazy {
        if (pluginSource == PluginSource.FILE && context.loader is FilePluginLoader) {
            (context.loader as FilePluginLoader).pluginFile.absolutePath
        } else {
            classpath.joinToString(File.pathSeparator).trim(File.pathSeparatorChar)
        } + File.pathSeparator
    }

    val paperVisionProjectManager by lazy {
        PaperVisionProjectManager(
            fullClasspath, fileSystem, engine, this, eocvSimApi
        )
    }

    val paperVisionTabPanel by lazy {
        PaperVisionTabPanel(this, eocvSimApi, paperVisionProjectManager)
    }

    override fun onLoad() {
        paperVisionProjectManager.init()

        eocvSimApi.visualizerApi.creationHook.once {
            val switchablePanel = eocvSimApi.visualizerApi.sidebarApi

            switchablePanel.addTab(paperVisionTabPanel)

            switchablePanel.tabChangeHook {
                isRunningPreviewPipeline = false
                switchToNecessaryPipeline()
            }

            val fileNewMenu = eocvSimApi.visualizerApi.topMenuBarApi.fileMenuApi.findSubMenuByTitle("New")
            fileNewMenu?.addSeparator()

            val fileNewPaperVisionMenu = JMenu("PaperVision")

            val fileNewPaperVisionProject = JMenuItem("New Project")
            fileNewPaperVisionProject.addActionListener {
                paperVisionProjectManager.newProjectAsk(eocvSimApi.visualizerApi.frame!!)
            }

            fileNewPaperVisionMenu.add(fileNewPaperVisionProject)

            val filePaperVisionImport = JMenuItem("Import...")
            filePaperVisionImport.addActionListener {
                paperVisionProjectManager.importProjectAsk(eocvSimApi.visualizerApi.frame!!)
            }

            fileNewPaperVisionMenu.add(filePaperVisionImport)

            fileNewMenu?.addMenuItem(fileNewPaperVisionMenu)
        }

        eocvSimApi.mainLoopHook.once(this::recoverProjects)
        PaperVisionProcessRunner.onPaperVisionExitError.doOnce(this::recoverProjects)

        eocvSimApi.pipelineManagerApi.onPipelineChangeHook {
            switchToNecessaryPipeline()
        }

        PaperVisionProcessRunner.onPaperVisionStart {
            // abort papervision pipeline
            switchToNecessaryPipeline()

            eocvSimApi.mainLoopHook.once {
                eocvSimApi.pipelineManagerApi.changePipeline(0, force = true)
            }
        }

        PaperVisionProcessRunner.onPaperVisionExit {
            switchToNecessaryPipeline()

            currentPrevizSession?.stopPreviz()
            currentPrevizSession = null
        }
    }

    override fun onEnable() {
        eocvSimApi.pipelineManagerApi.addPipelineClass(
            PaperVisionDefaultPipeline::class.java,
            PipelineManagerApi.PipelineSource.CLASSPATH,
            hidden = true
        )

        engine.setMessageHandlerOf<TunerChangeValueMessage> {
            eocvSimApi.mainLoopHook.once {
                val field = currentPrevizSession?.latestVirtualReflect?.getLabeledField(message.label)

                if (field != null) {
                    val tunableField = tunableFieldOf(field)

                    tunableField?.setFieldValue(0, message.value)
                }

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<TunerChangeValuesMessage> {
            eocvSimApi.mainLoopHook.once {
                val field = currentPrevizSession?.latestVirtualReflect?.getLabeledField(message.label)

                if (field != null) {
                    val tunableField = tunableFieldOf(field)

                    for (i in message.values.indices) {
                        tunableField?.setFieldValue(i, message.values[i]!!)
                    }
                }

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<GetInputSourcesMessage> {
            eocvSimApi.mainLoopHook.once {
                respond(
                    InputSourcesListResponse(inputSourcesToData())
                )
            }
        }

        engine.setMessageHandlerOf<GetCurrentInputSourceMessage> {
            eocvSimApi.mainLoopHook.once {
                respond(StringResponse(eocvSimApi.inputSourceManagerApi.currentSource?.name ?: ""))
            }
        }

        engine.setMessageHandlerOf<SetInputSourceMessage> {
            eocvSimApi.mainLoopHook.once {
                eocvSimApi.inputSourceManagerApi.setInputSource(message.inputSource)
                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<OpenCreateInputSourceMessage> {
            eocvSimApi.mainLoopHook.once {
                eocvSimApi.visualizerApi.dialogFactoryApi.createSourceDialog(message.sourceType.toApi())

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<InputSourceListChangeListenerMessage> {
            val currentSourceAmount = eocvSimApi.inputSourceManagerApi.allSources.size

            eocvSimApi.mainLoopHook.once {
                if (eocvSimApi.inputSourceManagerApi.allSources.size > currentSourceAmount) {
                    respond(InputSourcesListResponse(inputSourcesToData()))
                }
            }
        }

        engine.setMessageHandlerOf<PrevizAskNameMessage> {
            respond(
                StringResponse(
                    paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "")?.toValidIdentifier()
                        ?: "Mack"
                )
            )
        }

        engine.setMessageHandlerOf<AskProjectGenClassNameMessage> {
            respond(
                StringResponse(
                    paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "")?.toValidIdentifier()
                        ?: "Mack"
                )
            )
        }

        engine.setMessageHandlerOf<PrevizStartMessage> {
            eocvSimApi.mainLoopHook.once {
                if (currentPrevizSession != null) {
                    logger.warn("Stopping current previz session ${currentPrevizSession?.sessionName} to start new one")
                    logger.warn("It was not stopped beforehand, make sure to stop previz sessions timely")

                    currentPrevizSession?.stopPreviz()
                }

                val streamer = EOCVSimEngineImageStreamer(
                    previzNameProvider = { currentPrevizSession!!.sessionName },
                    Size(
                        message.streamWidth.toDouble(),
                        message.streamHeight.toDouble()
                    ),
                    engine
                )

                currentPrevizSession = EOCVSimPrevizSession(
                    message.previzName,
                    eocvSimApi,
                    paperVisionProjectManager,
                    streamer,
                    message.sourceCode
                )

                logger.debug("Starting with new source code\n{}", message.sourceCode)

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<PrevizPingMessage> {
            eocvSimApi.mainLoopHook.once {
                if (currentPrevizSession == null || currentPrevizSession?.sessionName != message.previzName) {
                    respond(ErrorResponse("Previz is not running"))
                } else {
                    currentPrevizSession?.handlePrevizPing()

                    val stats = eocvSimApi.pipelineManagerApi.pollStatistics()

                    respond(PrevizStatisticsResponse(stats.avgFps.toFloat(), stats.avgPipelineTimeMs.toLong()))
                }
            }
        }

        engine.setMessageHandlerOf<PrevizStopMessage> {
            eocvSimApi.mainLoopHook.once {
                if (currentPrevizSession?.sessionName == message.previzName) {
                    currentPrevizSession?.stopPreviz()
                    currentPrevizSession = null
                }
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizSourceCodeMessage> {
            eocvSimApi.mainLoopHook.once {
                if (currentPrevizSession?.sessionName == message.previzName) {
                    currentPrevizSession!!.refreshPreviz(message.sourceCode)
                    logger.debug("Received source code\n{}", message.sourceCode)

                    respond(OkResponse())
                } else {
                    respond(ErrorResponse("No previz session with name ${message.previzName}"))
                }
            }
        }
    }

    private fun inputSourcesToData() = eocvSimApi.inputSourceManagerApi.allSources.map {
        IpcInputSourceData(
            it.name,
            it.data.type.toIpc(),
            it.creationTime
        )
    }.toTypedArray().apply { sortBy { it.timestamp } }

    override fun onDisable() {
        PaperVisionProcessRunner.stopPaperVision()

        currentPrevizSession?.stopPreviz()
        currentPrevizSession = null

        paperVisionProjectManager.closeCurrentProject()
    }

    private fun recoverProjects() {
        if (paperVisionProjectManager.recoveredProjects.isNotEmpty()) {
            PaperVisionDialogFactory.displayProjectRecoveryDialog(
                eocvSimApi.visualizerApi.frame!!, paperVisionProjectManager.recoveredProjects
            ) {
                for (recoveredProject in it) {
                    paperVisionProjectManager.recoverProject(recoveredProject)
                }

                if (it.isNotEmpty()) {
                    JOptionPane.showMessageDialog(
                        eocvSimApi.visualizerApi.frame!!,
                        "Successfully recovered ${it.size} unsaved project(s)",
                        "PaperVision Project Recovery",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }

                paperVisionProjectManager.deleteAllRecoveredProjects()
            }
        }
    }

    internal fun switchToNecessaryPipeline() {
        if (isRunningPreviewPipeline) return

        if (eocvSimApi.visualizerApi.sidebarApi.isActive(paperVisionTabPanel)) {
            if (currentPrevizSession?.previzRunning != true || !PaperVisionProcessRunner.isRunning) {
                isRunningPreviewPipeline = true

                eocvSimApi.mainLoopHook.once {
                    eocvSimApi.pipelineManagerApi.changePipeline(
                        eocvSimApi.pipelineManagerApi.getIndexOf(
                            PaperVisionDefaultPipeline::class.java,
                            PipelineManagerApi.PipelineSource.CLASSPATH
                        ) ?: 0
                    )
                }
            }
        } else {
            // eocvSim.visualizer.viewport.renderer.setFpsMeterEnabled(true)
            isRunningPreviewPipeline = false
        }
    }

    private fun tunableFieldOf(field: VirtualField): TunableFieldApi? {
        if (tunableFieldCache.containsKey(field)) {
            return tunableFieldCache[field]!!
        }
        val tunableField = eocvSimApi.variableTunerApi.newTunableFieldInstanceOf(field, currentPrevizSession!!.latestPipeline!!)

        tunableFieldCache[field] = tunableField
        return tunableField
    }
}

fun IpcInputSourceType.toApi() = when(this) {
    IpcInputSourceType.IMAGE -> InputSourceApi.Type.IMAGE
    IpcInputSourceType.VIDEO -> InputSourceApi.Type.VIDEO
    IpcInputSourceType.CAMERA -> InputSourceApi.Type.CAMERA
    IpcInputSourceType.HTTP -> InputSourceApi.Type.HTTP
}
fun InputSourceApi.Type.toIpc() = when(this) {
    InputSourceApi.Type.IMAGE -> IpcInputSourceType.IMAGE
    InputSourceApi.Type.VIDEO -> IpcInputSourceType.VIDEO
    InputSourceApi.Type.CAMERA -> IpcInputSourceType.CAMERA
    InputSourceApi.Type.HTTP -> IpcInputSourceType.HTTP
}