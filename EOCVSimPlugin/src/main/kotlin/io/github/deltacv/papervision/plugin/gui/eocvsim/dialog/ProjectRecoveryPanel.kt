package io.github.deltacv.papervision.plugin.gui.eocvsim.dialog

import io.github.deltacv.papervision.plugin.project.recovery.RecoveredProject
import java.awt.BorderLayout
import java.awt.Dimension
import java.time.Instant
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ProjectRecoveryPanel(
    recoveredProjects: List<RecoveredProject>, // List of (Project Name, Date)
    callback: (List<RecoveredProject>) -> Unit
) : JPanel() {

    init {
        layout = BorderLayout(10, 10)
        preferredSize = Dimension(400, 250)

        // Add a descriptive label at the top
        val label = JLabel("<html><div style='text-align: center;'><h3>PaperVision detected unsaved projects from a previous session.<br>You can choose which projects to recover below.</h3></div></html>")
        label.horizontalAlignment = SwingConstants.CENTER // Center align the text

        add(label, BorderLayout.NORTH)

        // Data for the table, including the checkbox column initialized to true
        val data = recoveredProjects.map { arrayOf(it.originalProjectPath, Date.from(Instant.ofEpochMilli(it.date)), true) }.toTypedArray()
        val model = DefaultTableModel(data, arrayOf("Project Name", "Date", "Recover"))

        // Create the table with the custom model
        val table = object: JTable(model) {
            override fun getColumnClass(column: Int) = when (column) {
                2 -> java.lang.Boolean::class.java
                else -> String::class.java
            }

            override fun isCellEditable(row: Int, column: Int) = column == 2
        }
        val scrollPane = JScrollPane(table)
        scrollPane.border = BorderFactory.createEmptyBorder(-5, 10, 0, 10)
        add(scrollPane, BorderLayout.CENTER)

        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

            add(Box.createHorizontalGlue())

            add(JButton("Recover Selected").apply {
                addActionListener {
                    val selectedProjects = mutableListOf<RecoveredProject>()

                    for (i in 0 until model.rowCount) {
                        val recover = model.getValueAt(i, 2) as Boolean
                        if (recover) {
                            selectedProjects.add(recoveredProjects[i])
                        }
                    }

                    callback(selectedProjects.toList())
                    SwingUtilities.getWindowAncestor(this@ProjectRecoveryPanel).isVisible = false
                }
            })

            add(Box.createRigidArea(Dimension(10, 0)))

            add(JButton("Discard All").apply {
                addActionListener {
                    JOptionPane.showConfirmDialog(
                        this@ProjectRecoveryPanel,
                        "Are you sure you want to discard all recovered projects?",
                        "Discard All Projects",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    ).takeIf { it == JOptionPane.YES_OPTION }?.run {
                        callback(emptyList())
                        SwingUtilities.getWindowAncestor(this@ProjectRecoveryPanel).isVisible = false
                    }
                }
            })

            add(Box.createHorizontalGlue())
        }

        add(buttonsPanel, BorderLayout.SOUTH)
    }

}