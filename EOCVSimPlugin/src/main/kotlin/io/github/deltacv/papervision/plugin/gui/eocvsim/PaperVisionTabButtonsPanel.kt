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

    val newProjectBtt  = JButton("New Project")
    val deleteProjectBtt = JButton("Delete Selection")

    val openSelectionBtt = JButton("Open Selected Project")

    init {
        add(newProjectBtt, GridBagConstraints().apply {
            insets = Insets(0, 0, 0, 5)
        })

        newProjectBtt.addActionListener {
            PaperVisionDialogFactory.displayNewProjectDialog(
                SwingUtilities.getWindowAncestor(this) as JFrame,
                projectManager.projectTree.projects,
                projectManager.projectTree.folders,
            ) { projectGroup, projectName ->
                projectManager.newProject(projectGroup ?: "", projectName)

                JOptionPane.showConfirmDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Do you wish to open the project that was just created?",
                    "Project Created",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                ).takeIf { it == JOptionPane.YES_OPTION }?.run {
                    projectManager.openProject(projectManager.findProject(projectGroup ?: "", "$projectName.paperproj")!!)
                }
            }
        }

        add(deleteProjectBtt, GridBagConstraints().apply { gridx = 1 })

        openSelectionBtt.addActionListener {
            val selectedProject = projectsJTree.lastSelectedPathComponent
            if(selectedProject !is DefaultMutableTreeNode || selectedProject.userObject !is PaperVisionProjectTree.ProjectTreeNode.Project)
                return@addActionListener

            projectManager.openProject(selectedProject.userObject as PaperVisionProjectTree.ProjectTreeNode.Project)
        }

        deleteProjectBtt.addActionListener {
            if(projectsJTree.selectionPaths == null) return@addActionListener

            JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                "Are you sure you want to delete the selected project(s)?",
                "Delete Project",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ).takeIf { it == JOptionPane.YES_OPTION }?.let {
                val toDelete = mutableListOf<PaperVisionProjectTree.ProjectTreeNode.Project>()

                for(selection in projectsJTree.selectionPaths!!) {
                    val selectedProject = selection.lastPathComponent
                    if(selectedProject !is DefaultMutableTreeNode || selectedProject.userObject !is PaperVisionProjectTree.ProjectTreeNode.Project)
                        continue

                    if(toDelete.contains(selectedProject.userObject)) continue
                    toDelete.add(selectedProject.userObject as PaperVisionProjectTree.ProjectTreeNode.Project)
                }

                projectManager.bulkDeleteProjects(*toDelete.toTypedArray())
            }
        }

        projectsJTree.addTreeSelectionListener {
            val selectedProject = projectsJTree.lastSelectedPathComponent

            val state = selectedProject is DefaultMutableTreeNode &&
                    selectedProject.userObject is PaperVisionProjectTree.ProjectTreeNode.Project

            deleteProjectBtt.isEnabled = state
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


        deleteProjectBtt.isEnabled = false
        openSelectionBtt.isEnabled = false
    }

}