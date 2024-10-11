package io.github.deltacv.papervision.plugin.gui.eocvsim

import io.github.deltacv.papervision.plugin.project.PaperVisionProjectTree
import java.awt.Component
import javax.swing.JTree
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class ProjectTreeCellRenderer: DefaultTreeCellRenderer() {

    override fun getTreeCellRendererComponent(tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
        val component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

        if(value is DefaultMutableTreeNode) {
            val node = value.userObject

            if(node is PaperVisionProjectTree.ProjectTreeNode.Project) {
                icon = UIManager.getIcon("FileView.fileIcon")
            } else if(node is PaperVisionProjectTree.ProjectTreeNode.Folder) {
                icon = UIManager.getIcon("FileView.directoryIcon")
            }
        }

        return component
    }

}