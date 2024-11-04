package io.github.deltacv.eocvsim.pipeline

import io.github.deltacv.eocvsim.stream.ImageStreamer

internal object StreamableOpenCvPipelineAccessor {

    fun setStreamerOf(pipeline: StreamableOpenCvPipeline, streamer: ImageStreamer) {
        pipeline.streamer = streamer
    }

}