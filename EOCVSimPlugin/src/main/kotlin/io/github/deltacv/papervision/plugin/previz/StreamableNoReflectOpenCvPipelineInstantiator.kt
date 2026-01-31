package io.github.deltacv.papervision.plugin.previz

import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipeline
import com.github.serivesmejia.eocvsim.pipeline.instantiator.DefaultPipelineInstantiator
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.PipelineInstantiatorApi
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import io.github.deltacv.papervision.util.loggerForThis
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class StreamableNoReflectOpenCvPipelineInstantiator(
    owner: EOCVSimPlugin, val imageStreamer: ImageStreamer
) : PipelineInstantiatorApi(owner) {

    val logger by loggerForThis()

    override fun instantiatePipeline(pipelineClass: Class<*>, telemetry: Telemetry) = apiImpl {
        DefaultPipelineInstantiator.instantiate(pipelineClass, telemetry).apply {
            if (this is StreamableOpenCvPipeline) {
                this.streamer = imageStreamer
            }
        }
    }

    override fun virtualReflectionFor(pipeline: OpenCvPipeline) = apiImpl { JvmVirtualReflection }

    override fun variableTunerTargetFor(pipeline: OpenCvPipeline) = apiImpl { Any() }

    override fun disableApi() { }
}
