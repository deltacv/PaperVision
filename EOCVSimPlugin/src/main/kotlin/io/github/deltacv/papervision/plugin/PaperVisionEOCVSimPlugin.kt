package io.github.deltacv.papervision.plugin

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.papervision.engine.LocalPaperVisionEngine
import io.github.deltacv.papervision.engine.bridge.LocalPaperVisionEngineBridge
import io.github.deltacv.papervision.engine.client.message.*
import io.github.deltacv.papervision.engine.client.response.BooleanResponse
import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp
import io.github.deltacv.papervision.plugin.eocvsim.PrevizSession
import org.opencv.core.Size
import javax.swing.JButton
import javax.swing.JPanel

class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val engine = LocalPaperVisionEngine()

    var currentPrevizSession: PrevizSession? = null

    private val sessionStreamResolutions = mutableMapOf<String, Size>()

    override fun onLoad() {
        PaperVisionDaemon.launchDaemonPaperVision {
            PaperVisionApp(true, LocalPaperVisionEngineBridge(engine))
        }

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val panel = JPanel()
            panel.add(JButton("Start PaperVision").apply {
                addActionListener {
                    PaperVisionDaemon.invokeOnMainLoop {
                        PaperVisionDaemon.paperVision.window.visible = true
                    }
                }
            })

            eocvSim.visualizer.pipelineOpModeSwitchablePanel.add("PaperVision", panel)
        }
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

        engine.setMessageHandlerOf<PrevizPingPongMessage> {
            respond(BooleanResponse(
                currentPrevizSession?.sessionName == message.previzName && currentPrevizSession?.previzRunning ?: false)
            )
        }

        engine.setMessageHandlerOf<PrevizSetStreamResolutionMessage> {
            sessionStreamResolutions[message.previzName] = Size(message.width.toDouble(), message.height.toDouble())
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
            }

            currentPrevizSession!!.refreshPreviz(message.sourceCode)
            respond(OkResponse())
        }

        eocvSim.onMainUpdate {
            PaperVisionDaemon.watchdog()
            engine.process()
        }
    }

    override fun onDisable() {
    }

}