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

package io.github.deltacv.papervision.plugin.gui.eocvsim.dialog

import io.github.deltacv.papervision.plugin.project.recovery.RecoveredProject
import java.awt.Dialog
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

object PaperVisionDialogFactory {

    fun displayNewProjectDialog(parent: Window, projects: List<String>, groups: List<String>, name: String? = null, callback: (String?, String) -> Unit) {
        val panel = CreateNewProjectPanel(projects, groups, name, callback)
        displayDialog("${if(name == null) "New" else "Import"} Project", panel, parent)
    }

    fun displayProjectRecoveryDialog(parent: Window, recoveredProjects: List<RecoveredProject>, callback: (List<RecoveredProject>) -> Unit) {
        val panel = ProjectRecoveryPanel(recoveredProjects, callback)
        displayDialog("Recover PaperVision Projects", panel, parent)
    }

    private fun displayDialog(title: String, panel: JPanel, parent: Window) {
        SwingUtilities.invokeLater {
            val dialog = JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL)

            dialog.add(panel)
            dialog.pack()
            dialog.setLocationRelativeTo(null)

            dialog.isVisible = true
        }
    }

}
