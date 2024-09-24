package io.github.deltacv.papervision.plugin

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.qualcomm.robotcore.util.ElapsedTime
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.papervision.engine.LocalPaperVisionEngine
import io.github.deltacv.papervision.engine.bridge.LocalPaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.message.*
import io.github.deltacv.papervision.engine.client.response.BooleanResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.plugin.eocvsim.PaperVisionDefaultPipeline
import io.github.deltacv.papervision.plugin.eocvsim.PrevizSession
import io.github.deltacv.papervision.plugin.gui.CloseConfirmWindow
import io.github.deltacv.papervision.plugin.gui.eocvsim.PaperVisionTabPanel
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.replaceLast
import org.opencv.core.Size
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val logger by loggerForThis()

    val engine = LocalPaperVisionEngine()

    var currentPrevizSession: PrevizSession? = null

    val paperVisionProjectManager = PaperVisionProjectManager(
        context.loader.pluginFile, fileSystem, eocvSim
    )

    val onEOCVSimUpdate = PaperVisionEventHandler("PaperVisionEOCVSimPlugin-OnEOCVSimUpdate")

    private val sessionStreamResolutions = mutableMapOf<String, Size>()

    private val previzPingPongTimer = ElapsedTime()

    override fun onLoad() {
        eocvSim.onMainUpdate {
            onEOCVSimUpdate.run()
        }

        PaperVisionDaemon.onAppInstantiate {
            PaperVisionDaemon.invokeOnMainLoop {
                paperVisionProjectManager.init()

                if(paperVisionProjectManager.recoveredProjects.isNotEmpty()) {
                    SwingUtilities.invokeLater {
                        PaperVisionDialogFactory.displayProjectRecoveryDialog(
                            eocvSim.visualizer.frame, paperVisionProjectManager.recoveredProjects
                        ) {
                            for (recoveredProject in it) {
                                paperVisionProjectManager.recoverProject(recoveredProject)
                            }

                            if(it.isNotEmpty()) {
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
        }

        PaperVisionDaemon.launchDaemonPaperVision {
            PaperVisionApp(
                true,
                LocalPaperVisionEngineBridge(engine),
                onEOCVSimUpdate,
                ::paperVisionUserCloseListener
            )
        }

        eocvSim.pipelineManager.requestAddPipelineClass(PaperVisionDefaultPipeline::class.java, PipelineSource.CLASSPATH)

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

            switchablePanel.addTab("PaperVision", PaperVisionTabPanel(paperVisionProjectManager))

            switchablePanel.addChangeListener {
                changeToPaperVisionPipelineIfNecessary()
            }
        }

        eocvSim.pipelineManager.onPipelineChange {
            changeToPaperVisionPipelineIfNecessary()
        }
    }

    private fun paperVisionUserCloseListener(): Boolean {
        if(paperVisionProjectManager.currentProject != null) {
            PaperVisionDaemon.paperVision.onUpdate.doOnce {
                CloseConfirmWindow {
                    when (it) {
                        CloseConfirmWindow.Action.YES -> {
                            paperVisionProjectManager.saveCurrentProject()
                            closePaperVision()
                        }
                        CloseConfirmWindow.Action.NO -> {
                            paperVisionProjectManager.discardCurrentRecovery()
                            closePaperVision()
                        }
                        else -> { /* NO-OP */ }
                    }
                }.enable()
            }

            return false
        } else return true
    }

    override fun onEnable() {
        engine.setMessageHandlerOf<TunerChangeValueMessage> {
            eocvSim.tunerManager.getTunableFieldWithLabel(message.label)?.setFieldValue(message.index, message.value)
            respond(OkResponse())
        }

        engine.setMessageHandlerOf<TunerChangeValuesMessage> {
            for(i in message.values.indices) {
                eocvSim.tunerManager.getTunableFieldWithLabel(message.label)?.setFieldValue(i, message.values[i])
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizAskNameMessage> {
            respond(StringResponse(
                paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "") ?: "Mack"
            ))
        }

        engine.setMessageHandlerOf<AskProjectGenClassName> {
            respond(StringResponse(
                paperVisionProjectManager.currentProject?.name?.replaceLast(".paperproj", "") ?: "Mack"
            ))
        }

        engine.setMessageHandlerOf<PrevizPingPongMessage> {
            if(currentPrevizSession?.sessionName == message.previzName) {
                previzPingPongTimer.reset()
            }

            respond(BooleanResponse(
                currentPrevizSession?.sessionName == message.previzName && currentPrevizSession?.previzRunning ?: false)
            )
        }

        engine.setMessageHandlerOf<PrevizStopMessage> {
            if(currentPrevizSession?.sessionName == message.previzName) {
                currentPrevizSession?.stopPreviz()
                currentPrevizSession = null
            }
            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizSetStreamResolutionMessage> {
            sessionStreamResolutions[message.previzName] = Size(message.width.toDouble(), message.height.toDouble())

            if(currentPrevizSession?.sessionName == message.previzName) {
                currentPrevizSession?.streamer?.resolution = Size(message.width.toDouble(), message.height.toDouble())
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizSourceCodeMessage> {
            if(currentPrevizSession == null || currentPrevizSession?.sessionName != message.previzName) {
                currentPrevizSession = PrevizSession(
                    message.previzName,
                    engine, eocvSim,
                    sessionStreamResolutions[message.previzName] ?: Size(320.0, 240.0)
                )

                currentPrevizSession!!.startPreviz(message.sourceCode)

                previzPingPongTimer.reset()
            }

            currentPrevizSession!!.refreshPreviz(message.sourceCode)
            respond(OkResponse())

            logger.info("Received source code\n{}", message.sourceCode)
        }

        eocvSim.onMainUpdate {
            engine.process()

            if(previzPingPongTimer.seconds() > 5.0 && currentPrevizSession != null) {
                logger.info("Previz session ${currentPrevizSession?.sessionName} timed out, stopping")
                currentPrevizSession?.stopPreviz()
                currentPrevizSession = null
            }
        }
    }

    override fun onDisable() {
    }

    private fun closePaperVision() {
        paperVisionProjectManager.closeCurrentProject()
        PaperVisionDaemon.hidePaperVision()

        SwingUtilities.invokeLater {
            eocvSim.visualizer.frame.isVisible = true
        }

        currentPrevizSession?.stopPreviz()
        currentPrevizSession = null
        changeToPaperVisionPipelineIfNecessary()
    }

    private fun changeToPaperVisionPipelineIfNecessary() {
        val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

        if(switchablePanel.selectedIndex == switchablePanel.indexOfTab("PaperVision")) {
            if(currentPrevizSession?.previzRunning != true) {
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
        }
    }

}