package io.github.deltacv.papervision.plugin.eocvsim

import com.github.serivesmejia.eocvsim.util.ReflectUtil
import org.codehaus.janino.JavaSourceClassLoader
import org.codehaus.janino.SimpleCompiler
import org.openftc.easyopencv.OpenCvPipeline
import java.util.*
import javax.tools.*


object SinglePipelineCompiler {

    /**
     * Takes source code and returns a compiled pipeline class
     */
    @Suppress("UNCHECKED_CAST")
    fun compilePipeline(pipelineName: String, pipelineSource: String): Class<out OpenCvPipeline> {
        // Create a SimpleCompiler instance
        val compiler = SimpleCompiler()

        // Set the source code
        compiler.cook(pipelineSource)

        val clazz = compiler.classLoader.loadClass(pipelineName)

        require(ReflectUtil.hasSuperclass(clazz, OpenCvPipeline::class.java)) {
            "Pipeline class must extend OpenCvPipeline"
        }

        return clazz as Class<out OpenCvPipeline>
    }
}