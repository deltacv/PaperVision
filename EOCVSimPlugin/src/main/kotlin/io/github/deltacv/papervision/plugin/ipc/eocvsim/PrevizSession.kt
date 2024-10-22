package io.github.deltacv.papervision.plugin.ipc.eocvsim

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipelineInstantiator
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.papervision.plugin.PaperVisionProcessRunner
import io.github.deltacv.papervision.plugin.eocvsim.SinglePipelineCompiler

class PrevizSession(
    val sessionName: String,
    val eocvSim: EOCVSim,
    val streamer: ImageStreamer = NoOpEngineImageStreamer
) {

    var previzRunning = false
        private set

    private var latestClass: Class<*>? = null

    val logger by loggerForThis()

    init {
        eocvSim.pipelineManager.onPipelineChange {
            if(latestClass == null) return@onPipelineChange
            val current = eocvSim.pipelineManager.currentPipeline ?: return@onPipelineChange

            if(previzRunning && current::class.java != latestClass) {
                eocvSim.pipelineManager.forceChangePipeline(eocvSim.pipelineManager.getIndexOf(latestClass!!, PipelineSource.COMPILED_ON_RUNTIME))
            }
        }

        eocvSim.pipelineManager.onPause {
            if(previzRunning) {
                eocvSim.pipelineManager.setPaused(false, PipelineManager.PauseReason.NOT_PAUSED)
            }
        }

        PaperVisionProcessRunner.onPaperVisionExit.doOnce {
            stopPreviz()
        }
    }

    fun startPreviz(sourceCode: String) {
        previzRunning = true

        logger.info("Starting previz session $sessionName")

        refreshPreviz(sourceCode)
    }

    fun refreshPreviz(sourceCode: String) {
        if(!previzRunning) return

        eocvSim.pipelineManager.onUpdate.doOnce {
            logger.info("Refreshing previz session $sessionName with new source code")

            val beforeClass = latestClass

            latestClass = SinglePipelineCompiler.compilePipeline(sessionName, sourceCode)

            eocvSim.pipelineManager.pipelines.removeAll { it.clazz == beforeClass }
            eocvSim.pipelineManager.refreshGuiPipelineList()

            eocvSim.pipelineManager.onUpdate.doOnce {
                eocvSim.pipelineManager.addPipelineClass(latestClass!!, PipelineSource.COMPILED_ON_RUNTIME)
                eocvSim.pipelineManager.addInstantiator(latestClass!!, StreamableOpenCvPipelineInstantiator(streamer))

                eocvSim.pipelineManager.forceChangePipeline(
                    eocvSim.pipelineManager.getIndexOf(
                        latestClass!!,
                        PipelineSource.COMPILED_ON_RUNTIME
                    )
                )
            }
        }
    }

    fun stopPreviz() {
        if(!previzRunning) return
        previzRunning = false

        logger.info("Stopping previz session $sessionName")

        if(streamer is EOCVSimEngineImageStreamer) {
            streamer.stop()
        }

        eocvSim.onMainUpdate.doOnce {
            eocvSim.pipelineManager.pipelines.removeAll { it.clazz == latestClass }
            eocvSim.pipelineManager.refreshGuiPipelineList()

            eocvSim.pipelineManager.forceChangePipeline(0)
        }
    }

}