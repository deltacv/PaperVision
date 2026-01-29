package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Window
import javax.swing.JButton
import javax.swing.JPanel

class NewProjectPanel(
    val projectManager: PaperVisionProjectManager,
    val ancestor: Window
) : JPanel() {

    val logger by loggerForThis()

    val newProjectBtt = JButton("New Project")

    val importProjectBtt = JButton("Import Project")

    init {
        layout = GridBagLayout()

        newProjectBtt.addActionListener {
            projectManager.newProjectAsk(ancestor)
        }

        add(newProjectBtt, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
        })

        importProjectBtt.addActionListener {
            projectManager.importProjectAsk(ancestor)
        }

        add(importProjectBtt, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
        })
    }

}
