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

package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.github.serivesmejia.eocvsim.gui.component.PopupX
import com.github.serivesmejia.eocvsim.gui.component.PopupX.Companion.popUpXOnThis
import com.github.serivesmejia.eocvsim.gui.util.Corner
import io.github.deltacv.papervision.plugin.PaperVisionProcessRunner
import io.github.deltacv.papervision.plugin.gui.eocvsim.dialog.PaperVisionDialogFactory
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectTree
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class PaperVisionTabButtonsPanel(
    projectsJTree: JTree,
    projectManager: PaperVisionProjectManager
) : JPanel(GridBagLayout()) {

    val newProjectBtt  = JButton("New")
    val editSelectionBtt = JButton("Edit")

    val openSelectionBtt = JButton("Open Selected Project")

    init {
        add(newProjectBtt, GridBagConstraints().apply {
            insets = Insets(0, 0, 0, 5)
        })

        var lastNewProjectPopup: PopupX? = null

        newProjectBtt.addActionListener {
            val location = newProjectBtt.locationOnScreen

            val window = SwingUtilities.getWindowAncestor(this)
            val popup = PopupX(window, NewProjectPanel(projectManager, SwingUtilities.getWindowAncestor(this)), location.x, location.y)

            popup.onShow {
                val popupX = location.x + (newProjectBtt.size.width / 2) - (popup.window.size.width / 2)
                val popupY = popup.window.location.y // Keep the original Y position, or update as needed

                popup.setLocation(popupX, popupY)
            }

            lastNewProjectPopup?.hide()
            popup.show()
            lastNewProjectPopup = popup
        }

        var lastEditSelectionPopup: PopupX? = null

        editSelectionBtt.addActionListener {
            if(projectsJTree.selectionPaths == null) return@addActionListener

            val projects = mutableListOf<PaperVisionProjectTree.ProjectTreeNode.Project>()

            for(selection in projectsJTree.selectionPaths!!) {
                val selectedProject = selection.lastPathComponent
                if(selectedProject !is DefaultMutableTreeNode || selectedProject.userObject !is PaperVisionProjectTree.ProjectTreeNode.Project)
                    continue

                if(projects.contains(selectedProject.userObject)) continue // avoid duplication
                projects.add(selectedProject.userObject as PaperVisionProjectTree.ProjectTreeNode.Project)
            }

            val location = editSelectionBtt.locationOnScreen

            val window = SwingUtilities.getWindowAncestor(this)
            val popup = PopupX(window, EditSelectionPanel(projects, projectManager, SwingUtilities.getWindowAncestor(this)), location.x, location.y)

            popup.onShow {
                val popupX = location.x + (editSelectionBtt.size.width / 2) - (popup.window.size.width / 2)
                val popupY = popup.window.location.y // Keep the original Y position, or update as needed

                popup.setLocation(popupX, popupY)
            }

            lastEditSelectionPopup?.hide()
            popup.show()
            lastEditSelectionPopup = popup
        }

        add(editSelectionBtt, GridBagConstraints().apply { gridx = 1 })

        openSelectionBtt.addActionListener {
            val selectedProject = projectsJTree.lastSelectedPathComponent
            if(selectedProject !is DefaultMutableTreeNode || selectedProject.userObject !is PaperVisionProjectTree.ProjectTreeNode.Project)
                return@addActionListener

            projectManager.requestOpenProject(selectedProject.userObject as PaperVisionProjectTree.ProjectTreeNode.Project)
        }

        projectsJTree.addTreeSelectionListener {
            val selectedProject = projectsJTree.lastSelectedPathComponent

            val state = selectedProject is DefaultMutableTreeNode &&
                    selectedProject.userObject is PaperVisionProjectTree.ProjectTreeNode.Project

            editSelectionBtt.isEnabled = state
            openSelectionBtt.isEnabled = state
        }

        add(openSelectionBtt, GridBagConstraints().apply {
            gridwidth = 2
            gridy = 1

            insets = Insets(5, 0, 0, 0)
            weightx = 1.0

            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
        })

        editSelectionBtt.isEnabled = false
        openSelectionBtt.isEnabled = false
    }

}