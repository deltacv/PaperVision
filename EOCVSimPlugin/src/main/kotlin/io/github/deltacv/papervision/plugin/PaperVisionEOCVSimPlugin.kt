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
import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipelineInstantiator
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import io.github.deltacv.papervision.engine.client.message.*
import io.github.deltacv.papervision.engine.client.response.ErrorResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.plugin.eocvsim.PaperVisionDefaultPipeline
import io.github.deltacv.papervision.plugin.gui.eocvsim.PaperVisionTabPanel
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.ipc.eocvsim.EOCVSimEngineImageStreamer
import io.github.deltacv.papervision.plugin.ipc.eocvsim.EOCVSimPrevizSession
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetInputSourcesMessage
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceData
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceListChangeListenerMessage
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceType
import io.github.deltacv.papervision.plugin.ipc.message.OpenCreateInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.SetInputSourceMessage
import io.github.deltacv.papervision.plugin.ipc.message.response.InputSourcesListResponse
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.util.replaceLast
import io.github.deltacv.papervision.util.toValidIdentifier
import io.javalin.Javalin
import org.eclipse.jetty.server.Server
import org.opencv.core.Size
import java.io.File
import java.util.WeakHashMap
import java.util.concurrent.Executors
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuItem
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

    var streamerServerPort = 0
        private set

    var currentPrevizSession: EOCVSimPrevizSession? = null

    private val tunableFieldCache = WeakHashMap<VirtualField, TunableField<*>>()

    /**
     * If the plugin comes from a file, we will just use the file classpath, since it's a single fat jar.
     * If the plugin comes from Maven, we will use the classpath of all the transitive dependencies.
     */
    val fullClasspath by lazy {
        if (pluginSource == PluginSource.FILE) {
            context.loader.pluginFile.absolutePath
        } else {
            classpath.joinToString(File.pathSeparator).trim(File.pathSeparatorChar)
        } + File.pathSeparator
    }

    val paperVisionProjectManager = PaperVisionProjectManager(
        fullClasspath, fileSystem, engine, eocvSim
    ) {
        if (streamerServerPort == 0) {
            waitForStreamerServerPort()
        }

        streamerServerPort
    }

    override fun onLoad() {
        paperVisionProjectManager.init()
        startJavalinServer()

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val switchablePanel = eocvSim.visualizer.pipelineOpModeSwitchablePanel

            switchablePanel.addTab("PaperVision", PaperVisionTabPanel(paperVisionProjectManager, eocvSim, switchablePanel))
            switchablePanel.setTabComponentAt(
                switchablePanel.indexOfTab("PaperVision"),
                JLabel("PaperVision", JLabel.CENTER)
            )

            switchablePanel.addChangeListener {
                changeToPaperVisionPipelineIfNecessary()
            }

            val fileNewMenu = eocvSim.visualizer.menuBar.mFileMenu.getMenuComponent(0) as JMenu
            fileNewMenu.addSeparator()

            val fileNewPaperVisionMenu = JMenu("PaperVision")

            val fileNewPaperVisionProject = JMenuItem("New Project")
            fileNewPaperVisionProject.addActionListener {
                paperVisionProjectManager.newProjectAsk(eocvSim.visualizer.frame)
            }

            fileNewPaperVisionMenu.add(fileNewPaperVisionProject)

            val filePaperVisionImport = JMenuItem("Import...")
            filePaperVisionImport.addActionListener {
                paperVisionProjectManager.importProjectAsk(eocvSim.visualizer.frame)
            }

            fileNewPaperVisionMenu.add(filePaperVisionImport)

            fileNewMenu.add(fileNewPaperVisionMenu)
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

    fun waitForStreamerServerPort(timeoutSeconds: Double = 10.0): Int {
        val start = System.currentTimeMillis()

        while (streamerServerPort == 0) {
            if (System.currentTimeMillis() - start >= timeoutSeconds * 1000) {
                logger.warn("Streamer server port not available after $timeoutSeconds seconds.")
                break
            }

            Thread.sleep(300)
        }

        return streamerServerPort
    }

    private fun attachJavalinHandlers(javalin: Javalin) {
        javalin.get("/") {
            it.redirect("/0")
        }.get("/available") { ctx ->

            val streamer = currentPrevizSession?.streamer

            if (streamer is EOCVSimEngineImageStreamer) {
                ctx.result(streamer.handlers().keys.joinToString(","))
            }
        }.get("/{id}") { ctx ->
            val streamer = currentPrevizSession?.streamer

            if (streamer is EOCVSimEngineImageStreamer) {
                val handler = try {
                    streamer.handlerFor(ctx.pathParam("id").toInt())
                } catch (_: Exception) {
                    null
                }

                handler?.handle(ctx)
            }

            ctx.result("Resource not found")
        }
    }

    private fun startJavalinServer() {
        var streamerServer: Javalin? = null

        Executors.newSingleThreadExecutor().execute {
            streamerServer = Javalin.create { config ->
                config.pvt.jetty.server = Server()
            }

            attachJavalinHandlers(streamerServer!!)

            streamerServer.start("127.0.0.1", 0)
        }

        Thread({
            while (true) {
                try {
                    if (streamerServer != null && streamerServer.jettyServer()?.port()!! >= 1) {
                        break
                    }
                } catch (_: Exception) {
                    continue
                }

                logger.info(
                    "Waiting for streamer server to start (current port ${
                        streamerServer?.jettyServer()?.port()
                    })..."
                )
                Thread.sleep(1000)
            }

            streamerServerPort = streamerServer.jettyServer().port()
            logger.info("Started streamer server in port $streamerServerPort")
        }, "StreamerServerPortWatcher-Thread").start()
    }

    override fun onEnable() {
        fun tunableFieldOf(field: VirtualField): TunableField<*> {
            if(tunableFieldCache.containsKey(field)) {
                return tunableFieldCache[field]!!
            }

            val tunableFieldClass = eocvSim.tunerManager.getTunableFieldOf(field)

            val tunableField = tunableFieldClass.getConstructor(
                Object::class.java,
                VirtualField::class.java,
                EOCVSim::class.java
            ).newInstance(
                currentPrevizSession!!.latestPipeline,
                field,
                eocvSim
            )

            tunableFieldCache[field] = tunableField
            return tunableField
        }

        engine.setMessageHandlerOf<TunerChangeValueMessage> {
            eocvSim.onMainUpdate.doOnce {
                val field = currentPrevizSession?.latestVirtualReflect?.getLabeledField(message.label)

                if (field != null) {
                    val tunableField = tunableFieldOf(field)

                    tunableField.setFieldValue(0, message.value)
                }

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<TunerChangeValuesMessage> {
            eocvSim.onMainUpdate.doOnce {
                val field = currentPrevizSession?.latestVirtualReflect?.getLabeledField(message.label)

                if(field != null) {
                    val tunableField = tunableFieldOf(field)

                    for (i in message.values.indices) {
                        tunableField.setFieldValue(i, message.values[i])
                    }
                }

                respond(OkResponse())
            }
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
                DialogFactory.createSourceDialog(
                    eocvSim, when (message.sourceType) {
                        InputSourceType.CAMERA -> SourceType.CAMERA
                        InputSourceType.VIDEO -> SourceType.VIDEO
                        else -> SourceType.IMAGE
                    }
                )
                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<InputSourceListChangeListenerMessage> {
            var currentSourceAmount = eocvSim.inputSourceManager.sources.size

            eocvSim.onMainUpdate {
                if (eocvSim.inputSourceManager.sources.size > currentSourceAmount) {
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
            eocvSim.onMainUpdate.doOnce {
                if (currentPrevizSession != null) {
                    logger.warn("Stopping current previz session ${currentPrevizSession?.sessionName} to start new one")
                    logger.warn("Please make sure to stop the previz session before starting a new one")

                    currentPrevizSession?.stopPreviz()
                }

                val streamer = EOCVSimEngineImageStreamer(
                    Size(
                        message.streamWidth.toDouble(),
                        message.streamHeight.toDouble()
                    )
                )

                currentPrevizSession = EOCVSimPrevizSession(
                    message.previzName,
                    eocvSim, streamer,
                    message.sourceCode
                )

                logger.info("Received source code\n{}", message.sourceCode)

                respond(OkResponse())
            }
        }

        engine.setMessageHandlerOf<PrevizPingMessage> {
            eocvSim.onMainUpdate.doOnce {
                if (currentPrevizSession == null || currentPrevizSession?.sessionName != message.previzName) {
                    respond(ErrorResponse("Previz is not running"))
                } else {
                    currentPrevizSession?.ensurePrevizPipelineRunning()
                    respond(OkResponse())
                }
            }
        }

        engine.setMessageHandlerOf<PrevizStopMessage> {
            eocvSim.onMainUpdate.doOnce {
                if (currentPrevizSession?.sessionName == message.previzName) {
                    currentPrevizSession?.stopPreviz()
                    currentPrevizSession = null
                }
            }

            respond(OkResponse())
        }

        engine.setMessageHandlerOf<PrevizSourceCodeMessage> {
            eocvSim.onMainUpdate.doOnce {
                if (currentPrevizSession?.sessionName == message.previzName) {
                    currentPrevizSession!!.refreshPreviz(message.sourceCode)
                    logger.info("Received source code\n{}", message.sourceCode)

                    respond(OkResponse())
                } else {
                    respond(ErrorResponse("No previz session with name ${message.previzName}"))
                }
            }
        }
    }

    private fun inputSourcesToData() = eocvSim.inputSourceManager.sources.map {
        InputSourceData(
            it.key,
            when (eocvSim.inputSourceManager.getSourceType(it.key)) {
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
                    eocvSim.pipelineManager.addInstantiator(
                        PaperVisionDefaultPipeline::class.java,
                        StreamableOpenCvPipelineInstantiator(EOCVSimEngineImageStreamer(Size(320.0, 240.0)))
                    )

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