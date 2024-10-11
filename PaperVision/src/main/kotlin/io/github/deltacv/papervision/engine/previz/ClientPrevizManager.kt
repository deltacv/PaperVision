package io.github.deltacv.papervision.engine.previz

import io.github.deltacv.papervision.codegen.CodeGenManager
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.engine.client.PaperVisionEngineClient
import io.github.deltacv.papervision.engine.client.message.PrevizSourceCodeMessage
import io.github.deltacv.papervision.engine.client.message.PrevizStartMessage
import io.github.deltacv.papervision.engine.client.message.PrevizStopMessage
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.io.TextureProcessorQueue
import io.github.deltacv.papervision.io.bufferedImageFromResource
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.loggerForThis

class ClientPrevizManager(
    val defaultPrevizStreamWidth: Int,
    val defaultPrevizStreamHeight: Int,
    val codeGenManager: CodeGenManager,
    val textureProcessorQueue: TextureProcessorQueue,
    val client: PaperVisionEngineClient
) {

    val offlineImages = arrayOf(
        bufferedImageFromResource("/img/TechnicalDifficulties.png"),
        bufferedImageFromResource("/img/PleaseHangOn.png")
    )

    var previzName: String? = null
        private set

    var stream = PipelineStream("", client, textureProcessorQueue, offlineImages = offlineImages)
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

                stream = PipelineStream(
                    previzName, client, textureProcessorQueue,
                    width = streamWidth, height = streamHeight,
                    offlineImages = offlineImages,
                    status = streamStatus
                )

                stream.start()
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
            stopPreviz()
        }

        startPreviz(previzName, previzStreamWidth, previzStreamHeight, status)
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
        previzRunning = false

        client.sendMessage(PrevizStopMessage(previzName!!))

        stream.stop()
        onPrevizStop.run()
    }

    fun update() {
        if(stream.popRequestedMaximize() && previzName != null) {
            logger.info("Maximizing previz session $previzName")

            restartWithStreamResolution(
                previzStreamWidth = stream.width * 2,
                previzStreamHeight = stream.height * 2,
                status = PipelineStream.Status.MAXIMIZED
            )
        }

        if(stream.popRequestedMinimize() && previzName != null) {
            logger.info("Minimizing previz session $previzName")

            restartWithStreamResolution(
                previzStreamWidth = stream.width / 2,
                previzStreamHeight = stream.height / 2,
                status = PipelineStream.Status.MINIMIZED
            )
        }
    }

}