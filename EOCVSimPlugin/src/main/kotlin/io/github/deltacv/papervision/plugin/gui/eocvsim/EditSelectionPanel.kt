package io.github.deltacv.papervision.plugin.gui.eocvsim

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.plus
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectManager
import io.github.deltacv.papervision.plugin.project.PaperVisionProjectTree
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Window
import java.io.File
import java.util.concurrent.CancellationException
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

class EditSelectionPanel(
    val targetProjects: List<PaperVisionProjectTree.ProjectTreeNode.Project>,
    val projectManager: PaperVisionProjectManager,
    val ancestor: Window
) : JPanel() {

    val deleteProjectBtt = JButton("Delete Project${if(targetProjects.size > 1) "s" else ""}")

    val exportProjectBtt = JButton("Export Project${if(targetProjects.size > 1) "s" else ""}")

    val cloneProjectBtt = JButton("Clone Selected Project")

    init {0
        layout = GridBagLayout()

        deleteProjectBtt.addActionListener {
            JOptionPane.showConfirmDialog(
                ancestor,
                "Are you sure you want to delete the selected project(s)?",
                "Delete Project",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ).takeIf { it == JOptionPane.YES_OPTION }?.let {
                projectManager.bulkDeleteProjects(*targetProjects.toTypedArray())
            }
        }

        add(deleteProjectBtt, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
        })

        exportProjectBtt.addActionListener {
            var nextDir: File? = null

            fun openFileChooserFor(project: PaperVisionProjectTree.ProjectTreeNode.Project) {
                JFileChooser().apply {
                    if(nextDir == null) {
                        nextDir = fileSystemView.defaultDirectory
                    }

                    fileFilter = FileNameExtensionFilter("PaperVision Project (.paperproj)", "paperproj")
                    selectedFile = nextDir!! + File.separator + project.name

                    if(showSaveDialog(ancestor) == JFileChooser.APPROVE_OPTION) {
                        val file = if(selectedFile.extension != "paperproj") {
                            selectedFile + ".paperproj"
                        } else selectedFile

                        if(file.exists()) {
                            val result = JOptionPane.showConfirmDialog(
                                ancestor,
                                "File already exists in the selected directory. Do you wish to replace it?"
                            )

                            if(result == JOptionPane.CANCEL_OPTION) {
                                throw CancellationException() // handle this later
                            } else if(result != JOptionPane.YES_OPTION) {
                                openFileChooserFor(project)
                                return@apply
                            }
                        }

                        SysUtil.saveFileStr(file, projectManager.readProjectFile(project))

                        nextDir = file.parentFile
                    } else {
                        throw CancellationException() // handle later as well
                    }
                }
            }

            for(project in targetProjects) {
                try {
                    openFileChooserFor(project)
                } catch (_: CancellationException) { // riiiiight here
                    break
                }
            }
        }

        add(exportProjectBtt, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
        })

        if(targetProjects.size == 1) {
            cloneProjectBtt.addActionListener { projectManager.cloneProjectAsk(targetProjects[0], ancestor) }

            add(cloneProjectBtt, GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                // fill
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                gridwidth = 2

                anchor = GridBagConstraints.CENTER

                insets = Insets(3, 0, 0, 0)
            })
        }
    }

}
