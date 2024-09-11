package io.github.deltacv.papervision.plugin

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.papervision.codegen.language.interpreted.LuaLanguage
import io.github.deltacv.papervision.plugin.eocvsim.LuaOpenCvPipeline
import io.github.deltacv.papervision.plugin.eocvsim.LuaOpenCvPipelineInstantiator
import javax.swing.JButton
import javax.swing.JPanel


class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

    val logger by loggerForThis()

    override fun onLoad() {
        PaperVisionDaemon.launchDaemonPaperVision()

        eocvSim.visualizer.onPluginGuiAttachment.doOnce {
            val panel = JPanel()
            panel.add(JButton("Start PaperVision").apply {
                addActionListener {
                    PaperVisionDaemon.invokeOnMainLoop {
                        PaperVisionDaemon.paperVision.window.visible = true
                    }
                }
            })

            eocvSim.visualizer.pipelineOpModeSwitchablePanel.add("PaperVision", panel)
        }

        eocvSim.onMainUpdate.doOnce {
            eocvSim.pipelineManager.addPipelineClass(LuaOpenCvPipeline::class.java, PipelineSource.CLASSPATH)
        }

        PaperVisionDaemon.attachToMainLoop {
            if(PaperVisionDaemon.paperVision.keyManager.released(PaperVisionDaemon.paperVision.setup.keys!!.Escape)) {
                eocvSim.pipelineManager.onUpdate.doOnce {
                    val source = PaperVisionDaemon.paperVision.codeGenManager.build("kkk", LuaLanguage)

                    eocvSim.pipelineManager.addInstantiator(LuaOpenCvPipeline::class.java, LuaOpenCvPipelineInstantiator(source) )

                    eocvSim.pipelineManager.forceChangePipeline(
                        eocvSim.pipelineManager.getIndexOf(LuaOpenCvPipeline::class.java, PipelineSource.CLASSPATH)
                    )
                }
            }
        }
    }

    override fun onEnable() {
    }

    override fun onDisable() {
    }

}