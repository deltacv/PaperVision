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

package io.github.deltacv.papervision.engine.previz

import io.github.deltacv.papervision.codegen.CodeGenManager
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.engine.client.ByteMessageReceiver
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.message.PrevizPingMessage
import io.github.deltacv.papervision.engine.client.message.PrevizSourceCodeMessage
import io.github.deltacv.papervision.engine.client.message.PrevizStartMessage
import io.github.deltacv.papervision.engine.client.message.PrevizStopMessage
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.io.bufferedImageFromResource
import io.github.deltacv.papervision.platform.PlatformTextureFactory
import io.github.deltacv.papervision.util.ElapsedTime
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.hexString
import io.github.deltacv.papervision.util.loggerForThis

class ClientPrevizManager(
    val defaultPrevizStreamWidth: Int,
    val defaultPrevizStreamHeight: Int,
    val codeGenManager: CodeGenManager,
    val client: PaperVisionEngineClient,
    val byteReceiverProvider: (() -> ByteMessageReceiver)? = null
) {

    val offlineImages = arrayOf(
        bufferedImageFromResource("/img/TechnicalDifficulties.png"),
        bufferedImageFromResource("/img/PleaseHangOn.png")
    )

    var previzName: String? = null
        private set

    var stream = PipelineStream("", client, offlineImages = offlineImages)
        private set(value) {
            field = value
            onStreamChange.run()
        }

    val onPrevizStart = PaperVisionEventHandler("ClientPrevizManager-OnPrevizStart")
    val onPrevizStop = PaperVisionEventHandler("ClientPrevizManager-OnPrevizStop")

    val onStreamChange = PaperVisionEventHandler("ClientPrevizManager-OnStreamChange")

    val logger by loggerForThis()

    var previzRunning = false
        private set

    private val pingTimer = ElapsedTime()

    fun startPreviz(previzName: String) {
        startPreviz(previzName, JavaLanguage)
    }

    fun startPreviz(previzName: String, streamWidth: Int, streamHeight: Int, streamStatus: PipelineStream.Status) {
        startPreviz(previzName, codeGenManager.build(previzName, JavaLanguage, true), streamWidth, streamHeight, streamStatus)
    }

    fun startPreviz(previzName: String, language: Language) {
        startPreviz(previzName, codeGenManager.build(previzName, language, true))
    }

    fun startPreviz(
        previzName: String,
        sourceCode: String?,
        streamWidth: Int = defaultPrevizStreamWidth,
        streamHeight: Int = defaultPrevizStreamHeight,
        streamStatus: PipelineStream.Status = PipelineStream.Status.MINIMIZED
    ) {
        this.previzName = previzName

        if(sourceCode == null) {
            logger.warn("Failed to start previz session $previzName, source code is null (probably due to code gen error)")
            return
        }

        logger.info("Starting previz session $previzName")

        client.sendMessage(PrevizStartMessage(previzName, sourceCode, streamWidth, streamHeight).onResponseWith<OkResponse> {
            client.onProcess.doOnce {
                logger.info("Previz session $previzName running")

                previzRunning = true

                onPrevizStart.run()

                stream.stop()

                stream = if(byteReceiverProvider == null) {
                    PipelineStream(
                        previzName, client,
                        width = streamWidth, height = streamHeight,
                        offlineImages = offlineImages,
                        status = streamStatus
                    )
                } else {
                    PipelineStream(
                        previzName, byteReceiverProvider(),
                        width = streamWidth, height = streamHeight,
                        offlineImages = offlineImages,
                        status = streamStatus
                    )
                }

                stream.start()
                pingTimer.reset()
            }
        })
    }

    private fun restartWithStreamResolution(
        previzName: String = this.previzName!!,
        previzStreamWidth: Int = this.defaultPrevizStreamWidth,
        previzStreamHeight: Int = this.defaultPrevizStreamHeight,
        status: PipelineStream.Status = stream.status
    ) {
        if(previzRunning) {
            logger.info("Restarting previz session $previzName with new stream resolution")

            onPrevizStop.doOnce {
                startPreviz(previzName, previzStreamWidth, previzStreamHeight, status)
            }

            stopPreviz()
        } else {
            logger.info("Starting previz session $previzName with new stream resolution")
            startPreviz(previzName, previzStreamWidth, previzStreamHeight, status)
        }
    }

    fun refreshPreviz() = previzName?.let{
        refreshPreviz(codeGenManager.build(it, JavaLanguage, true))
    }

    fun refreshPreviz(sourceCode: String?) {
        if(sourceCode == null) {
            logger.warn("Failed to refresh previz session $previzName, source code is null (probably due to code gen error)")
            return
        }

        if(previzRunning)
            client.sendMessage(PrevizSourceCodeMessage(previzName!!, sourceCode))
    }

    fun stopPreviz() {
        logger.info("Stopping previz session $previzName")

        client.sendMessage(PrevizStopMessage(previzName!!).onResponseWith<OkResponse> {
            client.onProcess.doOnce {
                logger.info("Previz session $previzName stopped")
                stream.stop()

                previzRunning = false

                onPrevizStop.run()
            }
        })
    }

    fun update() {
        if(previzName != null && previzRunning && pingTimer.seconds > 2) {
            client.sendMessage(PrevizPingMessage(previzName!!))
            pingTimer.reset()
        }

        if(stream.popRequestedMaximize() && previzName != null && previzRunning) {
            logger.info("Maximizing previz session $previzName")

            restartWithStreamResolution(
                previzStreamWidth = stream.width * 2,
                previzStreamHeight = stream.height * 2,
                status = PipelineStream.Status.MAXIMIZED
            )
        }

        if(stream.popRequestedMinimize() && previzName != null && previzRunning) {
            logger.info("Minimizing previz session $previzName")

            restartWithStreamResolution(
                previzStreamWidth = stream.width / 2,
                previzStreamHeight = stream.height / 2,
                status = PipelineStream.Status.MINIMIZED
            )
        }
    }

}