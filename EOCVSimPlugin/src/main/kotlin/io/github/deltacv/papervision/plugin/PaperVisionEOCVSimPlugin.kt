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

import com.formdev.flatlaf.demo.HintManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import io.github.deltacv.papervision.engine.client.message.*
import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.plugin.eocvsim.PaperVisionDefaultPipeline
import io.github.deltacv.papervision.plugin.gui.eocvsim.PaperVisionTabPanel
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.ipc.eocvsim.EOCVSimEngineImageStreamer
import io.github.deltacv.papervision.plugin.ipc.eocvsim.PrevizSession
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetInputSourcesMessage
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceData
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceType
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceListChangeListenerMessage
import io.github.deltacv.papervision.plugin.ipc.message.OpenCreateInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.SetInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.util.replaceLast
import org.opencv.core.Size
import java.io.File
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Main entry point for the PaperVision plugin.
 * Specified in the plugin.toml file.
 */
class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val logger by loggerForThis()

    val engine = PaperVisionProcessRunner.paperVisionEngine

    var currentPrevizSession: PrevizSession? = null

    /**
     * If the plugin comes from a file, we will just use the file classpath, since it's a single fat jar.
     * If the plugin comes from Maven, we will use the classpath of all the transitive dependencies.
     */
    val fullClasspath = if(pluginSource == PluginSource.FILE) {
        context.loader.pluginFile.absolutePath
    } else {
        classpath.joinToString(File.pathSeparator).trim(File.pathSeparatorChar)
    }

    val paperVisionProjectManager = PaperVisionProjectManager(
        fullClasspath, fileSystem, engine, eocvSim
    )

    override fun onLoad() {
        paperVisionProjectManager.init()

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

            switchablePanel.addTab("PaperVision", PaperVisionTabPanel(paperVisionProjectManager))
            switchablePanel.setTabComponentAt(
                switchablePanel.indexOfTab("PaperVision"),
                JLabel("PaperVision", JLabel.CENTER)
            )

            val shouldShowHint = !eocvSim.config.flags.getOrElse("hasShownPaperVisionHint") { false }

            if(shouldShowHint) {
                eocvSim.onMainUpdate.doOnce {
                    SwingUtilities.invokeLater {
                        val hint = HintManager.Hint(
                            "Try out the new way to develop your vision pipelines!",
                            switchablePanel.getTabComponentAt(switchablePanel.indexOfTab("PaperVision")),
                            SwingConstants.BOTTOM, null
                        )

                        HintManager.showHint(hint)
                        eocvSim.config.flags["hasShownPaperVisionHint"] = true
                    }
                }
            }

            switchablePanel.addChangeListener {
                changeToPaperVisionPipelineIfNecessary()
            }
        }

        val recoveredProjectsListener = Runnable {
            if (paperVisionProjectManager.recoveredProjects.isNotEmpty()) {
                SwingUtilities.invokeLater {
                    PaperVisionDialogFactory.displayProjectRecoveryDialog(
                        eocvSim.visualizer.frame, paperVisionProjectManager.recoveredProjects
                    ) {
                        for (recoveredProject in it) {
                            paperVisionProjectManager.recoverProject(recoveredProject)
                        }

                        if (it.isNotEmpty()) {
                            JOptionPane.showMessageDialog(
                                eocvSim.visualizer.frame,
                                "Successfully recovered ${it.size} unsaved project(s)",
                                "PaperVision Project Recovery",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        }

                        paperVisionProjectManager.deleteAllRecoveredProjects()
                    }
                }
            }
        }

        eocvSim.onMainUpdate.doOnce(recoveredProjectsListener)
        PaperVisionProcessRunner.onPaperVisionExitError.doOnce(recoveredProjectsListener)

        eocvSim.pipelineManager.onPipelineChange {
            changeToPaperVisionPipelineIfNecessary()
        }

        PaperVisionProcessRunner.onPaperVisionExit {
            changeToPaperVisionPipelineIfNecessary()
        }
    }

    override fun onEnable() {
        engine.setMessageHandlerOf<TunerChangeValueMessage> {
            eocvSim.tunerManager.getTunableFieldWithLabel(message.label)?.setFieldValue(message.index, message.value)
            respond(OkResponse())
        }

        engine.setMessageHandlerOf<TunerChangeValuesMessage> {
            for (i in message.values.indices) {
                eocvSim.tunerManager.getTunableFieldWithLabel(message.label)?.setFieldValue(i, message.values[i])
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<GetInputSourcesMessage> {
            eocvSim.onMainUpdate.doOnce {
                respond(
                    InputSourcesListResponse(
                        inputSourcesToData()
                    )
                )
            }
        }

        engine.setMessageHandlerOf<GetCurrentInputSourceMessage> {
            eocvSim.onMainUpdate.doOnce {
                respond(StringResponse(eocvSim.inputSourceManager.currentInputSource?.name ?: ""))
            }
        }

        engine.setMessageHandlerOf<SetInputSourceMessage> {
            eocvSim.onMainUpdate.doOnce {
                eocvSim.inputSourceManager.requestSetInputSource(message.inputSource)
                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<OpenCreateInputSourceMessage> {
            eocvSim.onMainUpdate.doOnce {
                DialogFactory.createSourceDialog(eocvSim, when(message.sourceType) {
                    InputSourceType.CAMERA -> SourceType.CAMERA
                    InputSourceType.VIDEO -> SourceType.VIDEO
                    else -> SourceType.IMAGE
                })
                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<InputSourceListChangeListenerMessage> {
            var currentSourceAmount = eocvSim.inputSourceManager.sources.size

            eocvSim.onMainUpdate {
                if(eocvSim.inputSourceManager.sources.size > currentSourceAmount) {
                    respond(InputSourcesListResponse(inputSourcesToData()))
                }
            }
        }

        engine.setMessageHandlerOf<PrevizAskNameMessage> {
            respond(
                StringResponse(
                    paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "") ?: "Mack"
                )
            )
        }

        engine.setMessageHandlerOf<AskProjectGenClassNameMessage> {
            respond(
                StringResponse(
                    paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "") ?: "Mack"
                )
            )
        }

        engine.setMessageHandlerOf<PrevizStartMessage> {
            if (currentPrevizSession != null) {
                logger.warn("Stopping current previz session ${currentPrevizSession?.sessionName} to start new one")
                logger.warn("Please make sure to stop the previz session before starting a new one")
                currentPrevizSession?.stopPreviz()
            }

            val streamer = EOCVSimEngineImageStreamer(
                engine,
                message.previzName,
                Size(
                    message.streamWidth.toDouble(),
                    message.streamHeight.toDouble()
                )
            )

            currentPrevizSession = PrevizSession(
                message.previzName,
                eocvSim, streamer
            )

            currentPrevizSession?.startPreviz(message.sourceCode)

            logger.info("Received source code\n{}", message.sourceCode)

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizStopMessage> {
            if (currentPrevizSession?.sessionName == message.previzName) {
                currentPrevizSession?.stopPreviz()
                currentPrevizSession = null
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizSourceCodeMessage> {
            if (currentPrevizSession?.sessionName == message.previzName) {
                currentPrevizSession!!.refreshPreviz(message.sourceCode)
                logger.info("Received source code\n{}", message.sourceCode)

                respond(OkResponse())
            }

            respond(ErrorResponse("No previz session with name ${message.previzName}"))
        }
    }

    private fun inputSourcesToData() = eocvSim.inputSourceManager.sources.map {
        InputSourceData(
            it.key,
            when(eocvSim.inputSourceManager.getSourceType(it.key)) {
                SourceType.CAMERA -> InputSourceType.CAMERA
                SourceType.VIDEO -> InputSourceType.VIDEO
                else -> InputSourceType.IMAGE
            },
            it.value.creationTime
        )
    }.toTypedArray().apply { sortBy { it.timestamp } }

    override fun onDisable() {
    }

    private fun changeToPaperVisionPipelineIfNecessary() {
        val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

        if (switchablePanel.selectedIndex == switchablePanel.indexOfTab("PaperVision")) {
            if (currentPrevizSession?.previzRunning != true || !PaperVisionProcessRunner.isRunning) {
                eocvSim.pipelineManager.requestAddPipelineClass(
                    PaperVisionDefaultPipeline::class.java,
                    PipelineSource.CLASSPATH
                )

                eocvSim.pipelineManager.onUpdate.doOnce {
                    eocvSim.pipelineManager.changePipeline(
                        eocvSim.pipelineManager.getIndexOf(
                            PaperVisionDefaultPipeline::class.java,
                            PipelineSource.CLASSPATH
                        )!!
                    )

                    eocvSim.visualizer.viewport.renderer.setFpsMeterEnabled(false)
                }
            }
        } else {
            eocvSim.visualizer.viewport.renderer.setFpsMeterEnabled(true)

            eocvSim.pipelineManager.pipelines.removeAll { it.clazz == PaperVisionDefaultPipeline::class.java }
            eocvSim.pipelineManager.refreshGuiPipelineList()
        }
    }
}