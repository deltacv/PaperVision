package io.github.deltacv.papervision.plugin

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import javax.swing.JButton
import javax.swing.JPanel

class PaperVisionEOCVSimPlugin : EOCVSimPlugin() {

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
    }

    override fun onEnable() {
    }

    override fun onDisable() {
    }

}