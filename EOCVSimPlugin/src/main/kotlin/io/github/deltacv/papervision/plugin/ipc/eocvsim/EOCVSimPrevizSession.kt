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

package io.github.deltacv.papervision.plugin.ipc.eocvsim

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import io.github.deltacv.papervision.util.loggerForThis
import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipeline
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import io.github.deltacv.papervision.plugin.PaperVisionProcessRunner
import io.github.deltacv.papervision.plugin.eocvsim.SinglePipelineCompiler
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import org.openftc.easyopencv.OpenCvPipeline

class EOCVSimPrevizSession(
    val sessionName: String,
    val eocvSim: EOCVSim,
    val projectManager: PaperVisionProjectManager,
    val streamer: ImageStreamer = NoOpEngineImageStreamer,
    initialSourceCode: String
) {

    var previzRunning = false
        private set

    private var allClasses = mutableSetOf<Class<*>>()

    private var latestClass: Class<*>? = null
    private var latestSourceCode: String? = null

    var latestPipeline: OpenCvPipeline? = null
        private set
    var latestVirtualReflect: VirtualReflectContext? = null
        private set

    val logger by loggerForThis()

    private var isChangingPipeline = false

    init {
        eocvSim.pipelineManager.onPipelineChange {
            if (!previzRunning) {
                it.removeThis()
                return@onPipelineChange
            }

            if (latestClass == null) return@onPipelineChange

            if (isChangingPipeline) {
                return@onPipelineChange
            }

            val current = eocvSim.pipelineManager.currentPipeline ?: return@onPipelineChange

            if (previzRunning && current::class.java != latestClass) {
                // Temporarily disable the listener
                isChangingPipeline = true

                eocvSim.pipelineManager.forceChangePipeline(
                    eocvSim.pipelineManager.getIndexOf(
                        latestClass!!,
                        PipelineSource.COMPILED_ON_RUNTIME
                    )
                )

                latestPipeline = eocvSim.pipelineManager.currentPipeline

                latestVirtualReflect = if (latestPipeline != null) {
                    JvmVirtualReflection.contextOf(latestPipeline!!)
                } else {
                    null
                }

                // Re-enable the listener after the change
                isChangingPipeline = false
            }
        }

        eocvSim.pipelineManager.onPause {
            if (previzRunning) {
                eocvSim.pipelineManager.setPaused(false, PipelineManager.PauseReason.NOT_PAUSED)
            } else {
                it.removeThis()
            }
        }

        PaperVisionProcessRunner.onPaperVisionExit.doOnce {
            stopPreviz()
        }

        startPreviz(initialSourceCode)
    }

    private fun startPreviz(sourceCode: String) {
        previzRunning = true

        logger.info("Starting previz session $sessionName with streamer ${streamer.javaClass.simpleName}")

        refreshPreviz(sourceCode)
    }

    fun refreshPreviz(sourceCode: String) {
        if (!previzRunning) return

        projectManager.plugin.isRunningPreviewPipeline = false

        projectManager.saveLatestSource(sourceCode)

        eocvSim.pipelineManager.onUpdate.doOnce {
            logger.info("Refreshing previz session $sessionName with new source code")

            eocvSim.pipelineManager.pipelines.removeIf { it.clazz == latestClass }

            val newClass = SinglePipelineCompiler.compilePipeline(sourceCode)

            latestClass = newClass
            allClasses.add(newClass)

            latestSourceCode = sourceCode

            if (streamer is EOCVSimEngineImageStreamer) {
                streamer.refreshed()
            }

            eocvSim.pipelineManager.addInstantiator(
                newClass,
                StreamableNoReflectOpenCvPipelineInstantiator(streamer)
            )
            eocvSim.pipelineManager.addPipelineClass(newClass, PipelineSource.CLASSPATH)

            isChangingPipeline = true

            eocvSim.pipelineManager.onUpdate.doOnce {
                eocvSim.pipelineManager.forceChangePipeline(
                    eocvSim.pipelineManager.getIndexOf(
                        newClass,
                        PipelineSource.CLASSPATH
                    )
                )

                latestPipeline = eocvSim.pipelineManager.currentPipeline!!
                latestVirtualReflect = JvmVirtualReflection.contextOf(latestPipeline!!)

                isChangingPipeline = false
            }
        }
    }

    fun ensurePrevizPipelineRunning() {
        eocvSim.onMainUpdate.doOnce {
            if (!isChangingPipeline &&
                eocvSim.pipelineManager.currentPipeline?.javaClass?.name != sessionName &&
                latestSourceCode != null
            ) {
                refreshPreviz(latestSourceCode!!)
            }
        }
    }

    fun stopPreviz() {
        if (!previzRunning) return
        previzRunning = false

        logger.info("Stopping previz session $sessionName")

        if (streamer is EOCVSimEngineImageStreamer) {
            streamer.stop()
        }

        eocvSim.onMainUpdate.doOnce {
            eocvSim.pipelineManager.pipelines.removeAll { it.clazz in allClasses }
            eocvSim.pipelineManager.refreshGuiPipelineList()

            eocvSim.pipelineManager.forceChangePipeline(0)
        }
    }

}