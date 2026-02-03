/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.formdev.flatlaf.demo.HintManager
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel
import com.github.serivesmejia.eocvsim.plugin.api.impl.EOCVSimApiImpl
import com.github.serivesmejia.eocvsim.plugin.api.impl.VisualizerApiImpl
import io.github.deltacv.eocvsim.plugin.api.VisualizerSidebarApi
import io.github.deltacv.papervision.plugin.PaperVisionEOCVSimPlugin
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectTree
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class PaperVisionTabPanel(
    private val plugin: PaperVisionEOCVSimPlugin
) : VisualizerSidebarApi.Tab(plugin) {

    val root = DefaultMutableTreeNode("/")

    private var previousSelectedProjectNode: PaperVisionProjectTree.TreeNode.Project? = null
    val projectList = JTree(root)
    val projectButtonsPanel = PaperVisionTabButtonsPanel(projectList, plugin.paperVisionProjectManager)

    val projectListAndButtonsPanel = JPanel()

    val sourceSelectorPanel = (plugin.eocvSimApi as? EOCVSimApiImpl)?.let {
        SourceSelectorPanel(it.internalEOCVSim)
    }

    override fun create(target: JPanel) = apiImpl {
        target.layout = GridBagLayout()

        projectListAndButtonsPanel.layout = GridBagLayout()
        projectListAndButtonsPanel.border = TitledBorder("Projects").apply {
            titleFont = titleFont.deriveFont(Font.BOLD)
            border = EmptyBorder(0, 0, 0, 0)
        }

        projectList.apply {
            isRootVisible = false
            showsRootHandles = true

            font = font.deriveFont(12f)

            cellRenderer = ProjectTreeCellRenderer()

            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    val node = projectList.lastSelectedPathComponent ?: return
                    if (node !is DefaultMutableTreeNode) return

                    val nodeObject = node.userObject

                    if (e.clickCount >= 2) {
                        if (nodeObject is PaperVisionProjectTree.TreeNode.Project) {
                            previousSelectedProjectNode = nodeObject
                            plugin.paperVisionProjectManager.requestOpenProject(nodeObject)
                        }
                    } else if (e.clickCount == 1 && previousSelectedProjectNode != nodeObject) {
                        if (nodeObject is PaperVisionProjectTree.TreeNode.Project) {
                            previousSelectedProjectNode = nodeObject
                            plugin.paperVisionProjectManager.previewProject(nodeObject)
                            setSourceSelectorEnabled(true)
                        } else {
                            plugin.paperVisionProjectManager.previewProject(null) // return to papervision default pipeline
                            setSourceSelectorEnabled(false)
                        }
                    }
                }
            })
        }

        val projectListScroll = JScrollPane()

        projectListScroll.setViewportView(projectList)

        plugin.paperVisionProjectManager.onRefresh {
            refreshProjectTree()
        }

        projectListAndButtonsPanel.add(projectListScroll, GridBagConstraints().apply {
            gridy = 0

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })

        projectListAndButtonsPanel.add(projectButtonsPanel, GridBagConstraints().apply {
            gridy = 1
            ipady = 20
        })

        target.add(projectListAndButtonsPanel, GridBagConstraints().apply {
            gridy = 0

            weightx = 0.5
            weighty = 0.8
            fill = GridBagConstraints.BOTH

            insets = Insets(10, 20, 5, 20)
        })

        if (sourceSelectorPanel != null) {
            sourceSelectorPanel.border = TitledBorder("Sources").apply {
                titleFont = titleFont.deriveFont(Font.BOLD)
                border = EmptyBorder(0, 0, 0, 0)
            }

            target.add(sourceSelectorPanel, GridBagConstraints().apply {
                gridy = 1

                weightx = 0.5
                weighty = 0.5
                fill = GridBagConstraints.BOTH

                insets = Insets(10, 20, 5, 20)
            })
        }

        refreshProjectTree()
    }

    fun refreshProjectTree() {
        val rootTree = plugin.paperVisionProjectManager.projectTree.rootTree.nodes

        SwingUtilities.invokeLater {
            root.removeAllChildren()

            if (rootTree.isNotEmpty()) {
                fun buildTree(folder: PaperVisionProjectTree.TreeNode.Folder): DefaultMutableTreeNode {
                    val folderNode = DefaultMutableTreeNode(folder)

                    for (node in folder.nodes) {
                        when (node) {
                            is PaperVisionProjectTree.TreeNode.Project -> {
                                folderNode.add(DefaultMutableTreeNode(node))
                            }

                            is PaperVisionProjectTree.TreeNode.Folder -> {
                                folderNode.add(buildTree(node))
                            }
                        }
                    }

                    return folderNode
                }

                for (node in rootTree) { // skip root "/" from showing up
                    if (node is PaperVisionProjectTree.TreeNode.Folder) {
                        root.add(buildTree(node))
                    } else if (node is PaperVisionProjectTree.TreeNode.Project) {
                        root.add(DefaultMutableTreeNode(node))
                    }
                }
            }

            (projectList.model as DefaultTreeModel).reload()
            projectList.revalidate()
        }
    }

    private fun setSourceSelectorEnabled(enabled: Boolean) {
        sourceSelectorPanel?.apply {
            sourceSelectorScroll.isEnabled = enabled
            allowSourceSwitching = enabled

            // Recursively enable/disable and adjust foreground color for graying effect
            fun setComponentTreeEnabled(component: java.awt.Component, enabled: Boolean) {
                component.isEnabled = enabled

                if (component is JComponent) {
                    if (!enabled) {
                        // Store original foreground if not already stored
                        if (component.getClientProperty("originalForeground") == null) {
                            component.putClientProperty("originalForeground", component.foreground)
                        }
                        // Apply grayed-out color
                        val original = component.getClientProperty("originalForeground") as? java.awt.Color
                        component.foreground = original?.let {
                            java.awt.Color(
                                (it.red + 128) / 2,
                                (it.green + 128) / 2,
                                (it.blue + 128) / 2,
                                it.alpha
                            )
                        }
                    } else {
                        // Restore original foreground
                        val original = component.getClientProperty("originalForeground") as? java.awt.Color
                        if (original != null) {
                            component.foreground = original
                        }
                    }
                }

                if (component is java.awt.Container) {
                    for (child in component.components) {
                        setComponentTreeEnabled(child, enabled)
                    }
                }
            }

            setComponentTreeEnabled(this, enabled)

            repaint()
        }
    }

    override val title = "PaperVision"

    override fun onActivated(): Unit = apiImpl {
        if (!plugin.eocvSimApi.configApi.hasFlag("hasShownPaperVisionHint")) {
            val hint = HintManager.Hint(
                "Create a new PaperVision project here",
                projectButtonsPanel.newProjectBtt,
                SwingConstants.TOP, null
            )

            HintManager.showHint(hint)

            plugin.eocvSimApi.configApi.putFlag("hasShownPaperVisionHint")
        }

        (plugin.eocvSimApi.visualizerApi as? VisualizerApiImpl)?.internalVisualizer?.viewport?.renderer?.setFpsMeterEnabled(false)

        setSourceSelectorEnabled(false)

        sourceSelectorPanel?.updateSourcesList()
    }

    override fun onDeactivated() = apiImpl {
        HintManager.hideAllHints()

        (plugin.eocvSimApi.visualizerApi as? VisualizerApiImpl)?.internalVisualizer?.viewport?.renderer?.setFpsMeterEnabled(true)

        setSourceSelectorEnabled(false)
    }
}