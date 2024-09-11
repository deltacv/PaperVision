package io.github.deltacv.papervision.plugin.eocvsim

import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class LuaOpenCvPipelineInstantiator(val source: String) : PipelineInstantiator {
    override fun instantiate(clazz: Class<*>, telemetry: Telemetry): OpenCvPipeline {
        require(clazz == LuaOpenCvPipeline::class.java) {
            "LuaOpenCvPipelineInstantiator can only instantiate LuaOpenCvPipeline"
        }

        return LuaOpenCvPipeline(source)
    }

    override fun variableTunerTargetObject(pipeline: OpenCvPipeline) = pipeline
}