package io.github.deltacv.papervision.plugin.gui.eocvsim.dialog

import io.github.deltacv.papervision.plugin.project.recovery.RecoveredProject
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

object PaperVisionDialogFactory {

    fun displayNewProjectDialog(parent: JFrame, projects: List<String>, groups: List<String>, callback: (String?, String) -> Unit) {
        val panel = NewProjectPanel(projects, groups, callback)
        displayDialog("New Project", panel, parent)
    }

    fun displayProjectRecoveryDialog(parent: JFrame, recoveredProjects: List<RecoveredProject>, callback: (List<RecoveredProject>) -> Unit) {
        val panel = ProjectRecoveryPanel(recoveredProjects, callback)
        displayDialog("Recover PaperVision Projects", panel, parent)
    }

    private fun displayDialog(title: String, panel: JPanel, parent: JFrame) {
        SwingUtilities.invokeLater {
            val dialog = JDialog(parent, title, true)

            dialog.add(panel)
            dialog.pack()
            dialog.setLocationRelativeTo(null)

            dialog.isVisible = true
        }
    }

}