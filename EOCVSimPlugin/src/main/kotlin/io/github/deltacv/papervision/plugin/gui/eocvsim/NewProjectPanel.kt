package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.github.serivesmejia.eocvsim.util.extension.removeFromEnd
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import java.awt.Dimension
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
        layout = BoxLayout(this, BoxLayout.LINE_AXIS)

        newProjectBtt.addActionListener {
            projectManager.newProjectAsk(SwingUtilities.getWindowAncestor(this) as JFrame)
        }

        add(newProjectBtt)

        add(Box.createRigidArea(Dimension(10, 1)))

        importProjectBtt.addActionListener {
            projectManager.importProjectAsk(SwingUtilities.getWindowAncestor(this) as JFrame)
        }

        add(importProjectBtt)
    }

}