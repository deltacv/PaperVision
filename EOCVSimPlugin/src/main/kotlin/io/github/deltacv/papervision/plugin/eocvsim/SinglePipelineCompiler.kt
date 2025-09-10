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
    fun compilePipeline(pipelineSource: String): Class<out OpenCvPipeline> {
        // Create a SimpleCompiler instance
        val compiler = SimpleCompiler()

        // Set the source code
        compiler.cook(pipelineSource)

        val clazz = compiler.classLoader.loadClass(compiler.classFiles.firstOrNull()?.thisClassName ?: throw IllegalStateException("No class found in compiled source"))

        require(ReflectUtil.hasSuperclass(clazz, OpenCvPipeline::class.java)) {
            "Pipeline class must extend OpenCvPipeline"
        }

        return clazz as Class<out OpenCvPipeline>
    }
}