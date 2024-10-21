package io.github.deltacv.papervision.plugin

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
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
import io.github.deltacv.papervision.plugin.ipc.message.OpenCreateInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.SetInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.replaceLast
import org.opencv.core.Size
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val logger by loggerForThis()

    val engine = PaperVisionProcessRunner.paperVisionEngine

    var currentPrevizSession: PrevizSession? = null

    val paperVisionProjectManager = PaperVisionProjectManager(
        context.loader.pluginFile, fileSystem, engine, eocvSim
    )

    val onEOCVSimUpdate = PaperVisionEventHandler("PaperVisionEOCVSimPlugin-OnEOCVSimUpdate")

    override fun onLoad() {
        eocvSim.onMainUpdate {
            onEOCVSimUpdate.run()
        }

        paperVisionProjectManager.init()

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

            switchablePanel.addTab("PaperVision", PaperVisionTabPanel(paperVisionProjectManager))

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
                        eocvSim.inputSourceManager.sources.map {
                            InputSourceData(
                                it.key,
                                when(eocvSim.inputSourceManager.getSourceType(it.key)) {
                                    SourceType.CAMERA -> InputSourceType.CAMERA
                                    SourceType.VIDEO -> InputSourceType.VIDEO
                                    else -> InputSourceType.IMAGE
                                }
                            )
                        }.toTypedArray()
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