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

import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectTree
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class PaperVisionTabPanel(
    val projectManager: PaperVisionProjectManager
) : JPanel() {

    val root = DefaultMutableTreeNode("Projects")

    val projectList = JTree(root)

    init {
        layout = GridBagLayout()

        projectList.apply {
            addMouseListener(object: MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if(e.clickCount >= 2) {
                        val node = projectList.lastSelectedPathComponent ?: return
                        if(node !is DefaultMutableTreeNode) return

                        val nodeObject = node.userObject
                        if(nodeObject !is PaperVisionProjectTree.ProjectTreeNode.Project) return

                        projectManager.requestOpenProject(nodeObject)
                    }
                }
            })

            cellRenderer = ProjectTreeCellRenderer()
        }

        val projectListScroll = JScrollPane()

        projectListScroll.setViewportView(projectList)

        projectManager.onRefresh {
            refreshProjectTree()
        }

        add(projectListScroll, GridBagConstraints().apply {
            gridy = 0

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })

        add(PaperVisionTabButtonsPanel(projectList, projectManager), GridBagConstraints().apply {
            gridy = 1
            ipady = 20
        })

        refreshProjectTree()
    }

    fun refreshProjectTree() {
        val rootTree = projectManager.projectTree.rootTree.nodes

        SwingUtilities.invokeLater {
            root.removeAllChildren()

            if(rootTree.isNotEmpty()) {
                fun buildTree(folder: PaperVisionProjectTree.ProjectTreeNode.Folder): DefaultMutableTreeNode {
                    val folderNode = DefaultMutableTreeNode(folder)

                    for (node in folder.nodes) {
                        when (node) {
                            is PaperVisionProjectTree.ProjectTreeNode.Project -> {
                                folderNode.add(DefaultMutableTreeNode(node))
                            }

                            is PaperVisionProjectTree.ProjectTreeNode.Folder -> {
                                folderNode.add(buildTree(node))
                            }
                        }
                    }

                    return folderNode
                }

                for (node in rootTree) { // skip root "/" from showing up
                    if (node is PaperVisionProjectTree.ProjectTreeNode.Folder) {
                        root.add(buildTree(node))
                    } else if (node is PaperVisionProjectTree.ProjectTreeNode.Project) {
                        root.add(DefaultMutableTreeNode(node))
                    }
                }
            }

            (projectList.model as DefaultTreeModel).reload()
            projectList.revalidate()
        }
    }
}