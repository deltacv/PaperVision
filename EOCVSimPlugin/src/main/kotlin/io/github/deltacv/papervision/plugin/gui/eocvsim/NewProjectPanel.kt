package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.github.serivesmejia.eocvsim.util.extension.removeFromEnd
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Window
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

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