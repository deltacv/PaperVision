package io.github.deltacv.papervision.plugin.ipc.eocvsim

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipelineInstantiator
import io.github.deltacv.eocvsim.stream.ImageStreamer
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

            latestClass = SinglePipelineCompiler.compilePipeline(sessionName, sourceCode)

            eocvSim.pipelineManager.removeAllPipelinesFrom(PipelineSource.COMPILED_ON_RUNTIME)

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
        previzRunning = false

        logger.info("Stopping previz session $sessionName")

        eocvSim.pipelineManager.removeAllPipelinesFrom(PipelineSource.COMPILED_ON_RUNTIME)
        eocvSim.pipelineManager.forceChangePipeline(0)
    }

}