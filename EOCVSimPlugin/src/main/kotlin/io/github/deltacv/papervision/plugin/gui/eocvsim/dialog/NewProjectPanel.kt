package io.github.deltacv.papervision.plugin.gui.eocvsim.dialog

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.nio.file.FileAlreadyExistsException
import javax.swing.*
import javax.swing.border.EmptyBorder

class NewProjectPanel(
    projects: List<String>,
    groups: List<String>,
    newProjectCallback: (group: String?, name: String) -> Unit
) : JPanel() {

    val projectNameField = JTextField()
    val groupComboBox = JComboBox<String>()

    init {
        preferredSize = Dimension(400, 100)
        layout = GridBagLayout()

        border = EmptyBorder(5, 15, 15, 15)

        add(JLabel("Project Name:").apply
        {
            horizontalAlignment = JLabel.RIGHT
            border = EmptyBorder(0,0,0,10)
        }, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            insets = Insets(10, 0, 0, 0)
        })

        add(projectNameField, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(10, 0, 0, 0)
        })

        add(JLabel("Group:").apply {
            horizontalAlignment = JLabel.RIGHT
            border = EmptyBorder(0,0,0,10)
        }, GridBagConstraints().apply {
            gridx = 0
            gridy = 1

            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(10, 0, 0, 0)
        })

        groupComboBox.addItem("None")

        for(group in groups) {
            if(group.isBlank() || group == "/") continue
            groupComboBox.addItem(group)
        }

        groupComboBox.selectedIndex = 0

        add(groupComboBox, GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(10, 0, 0, 0)
        })

        add(JButton("New Group").apply {
            addActionListener {
                val newGroup = JOptionPane.showInputDialog(this@NewProjectPanel, "Enter new group name:") ?: return@addActionListener
                (groupComboBox.model as DefaultComboBoxModel<String>).addElement(newGroup)
                groupComboBox.selectedItem = newGroup
            }
        }, GridBagConstraints().apply {
            gridx = 2
            gridy = 1
            insets = Insets(10, 5, 0, 0)
        })

        val buttonsPanel = JPanel().apply {
            border = EmptyBorder(5, 0, 0, 0)

            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())

            add(JButton("Create").apply {
                addActionListener {
                    if(!projects.contains(projectNameField.text)) {
                        if(projectNameField.text.trim().isNotBlank()) {
                            val projectName = groupComboBox.selectedItem!!.toString()

                            try {
                                newProjectCallback(
                                    if (projectName == "None") null else projectName,
                                    projectNameField.text
                                )
                            } catch(e: FileAlreadyExistsException) {
                                JOptionPane.showMessageDialog(this, "Project already exists")
                                return@addActionListener
                            }

                            SwingUtilities.getWindowAncestor(this).isVisible = false
                        } else {
                            JOptionPane.showMessageDialog(this, "Please enter a valid name")
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Project already exists")
                    }
                }
            })

            add(Box.createRigidArea(Dimension(10, 0)))

            add(JButton("Cancel").apply {
                addActionListener { SwingUtilities.getWindowAncestor(this).isVisible = false }
            })

            add(Box.createHorizontalGlue())
        }

        add(buttonsPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 2
            gridwidth = 3

            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
        })
    }

}