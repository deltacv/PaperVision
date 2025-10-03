package io.github.deltacv.papervision.plugin.ipc.eocvsim

import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipeline
import com.github.serivesmejia.eocvsim.pipeline.instantiator.DefaultPipelineInstantiator
import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import io.github.deltacv.papervision.util.loggerForThis
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class StreamableNoReflectOpenCvPipelineInstantiator(
    val imageStreamer: ImageStreamer
) : PipelineInstantiator {

    val logger by loggerForThis()

    override fun instantiate(clazz: Class<*>, telemetry: Telemetry) =
        DefaultPipelineInstantiator.instantiate(clazz, telemetry).apply {
            if(this is StreamableOpenCvPipeline) {
                this.streamer = imageStreamer
            }
        }

    override fun virtualReflectOf(pipeline: OpenCvPipeline) = JvmVirtualReflection

    override fun variableTunerTarget(pipeline: OpenCvPipeline) = Any()

}