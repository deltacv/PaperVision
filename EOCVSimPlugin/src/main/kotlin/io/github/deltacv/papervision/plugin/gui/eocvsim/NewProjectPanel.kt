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
            PaperVisionDialogFactory.displayNewProjectDialog(
                ancestor as JFrame,
                projectManager.projectTree.projects,
                projectManager.projectTree.folders,
            ) { projectGroup, projectName ->
                projectManager.newProject(projectGroup ?: "", projectName)

                JOptionPane.showConfirmDialog(
                    ancestor,
                    "Do you wish to open the project that was just created?",
                    "Project Created",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                ).takeIf { it == JOptionPane.YES_OPTION }?.run {
                    projectManager.openProject(projectManager.findProject(projectGroup ?: "", "$projectName.paperproj")!!)
                }
            }
        }

        add(newProjectBtt)

        add(Box.createRigidArea(Dimension(10, 1)))

        importProjectBtt.addActionListener {
            JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("PaperVision Project (.paperproj)", "paperproj")

                if(showSaveDialog(ancestor) == JFileChooser.APPROVE_OPTION) {
                    PaperVisionDialogFactory.displayNewProjectDialog(
                        ancestor as JFrame,
                        projectManager.projectTree.projects,
                        projectManager.projectTree.folders,
                        selectedFile.name.removeFromEnd(".paperproj")
                    ) { projectGroup, projectName ->
                        try {
                            projectManager.importProject(projectGroup ?: "", projectName, selectedFile)
                        } catch (e: java.nio.file.FileAlreadyExistsException) {
                            throw e // new project dialog will handle this
                        } catch(e: Exception) {
                            JOptionPane.showMessageDialog(ancestor, "Project file failed to load: ${e.javaClass.simpleName} ${e.message}")
                            logger.warn("Project file ${selectedFile.absolutePath} failed to load", e)
                            return@displayNewProjectDialog
                        }

                        JOptionPane.showConfirmDialog(
                            ancestor,
                            "Do you wish to open the project that was just imported?",
                            "Project Imported",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        ).takeIf { it == JOptionPane.YES_OPTION }?.run {
                            projectManager.openProject(projectManager.findProject(projectGroup ?: "", "$projectName.paperproj")!!)
                        }
                    }
                }
            }
        }

        add(importProjectBtt)
    }

}