package io.github.deltacv.papervision.plugin.project

import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import io.github.deltacv.papervision.plugin.PaperVisionDaemon
import io.github.deltacv.papervision.plugin.project.recovery.RecoveredProject
import io.github.deltacv.papervision.plugin.project.recovery.RecoveryDaemonProcessManager
import io.github.deltacv.papervision.plugin.project.recovery.RecoveryData
import io.github.deltacv.papervision.util.event.EventHandler
import io.github.deltacv.papervision.util.hexString
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.pathString

class PaperVisionProjectManager(
    pluginJarFile: File,
    val fileSystem: SandboxFileSystem
) {

    companion object {
        val recoveryFolder = EOCVSimFolder + File.separator + "recovery"
    }

    val root = fileSystem.getPath("")

    var projectTree = PaperVisionProjectTree(root)
        private set

    var projects = listOf<PaperVisionProjectTree.ProjectTreeNode.Project>()
        private set

    var currentProject: PaperVisionProjectTree.ProjectTreeNode.Project? = null
        private set

    fun paperVisionProjectFrom(
        project: PaperVisionProjectTree.ProjectTreeNode.Project,
        tree: JsonElement = PaperVisionDaemon.currentProjectJsonTree()
    ) = PaperVisionProject(
        Instant.now().toEpochMilli(),
        findProjectFolderPath(project)!!.pathString,
        project.name,
        tree
    )

    val recoveryDaemonProcessManager = RecoveryDaemonProcessManager(pluginJarFile).apply {
        start()
    }

    val logger by loggerForThis()

    val recoveredProjects = mutableListOf<RecoveredProject>().apply {
        if(recoveryFolder.exists()) {
            for(file in recoveryFolder.listFiles() ?: arrayOf()) {
                if(file.extension == "recoverypaperproj") {
                    val recoveredProject = RecoveredProject.fromJson(file.readText())
                    val projectPath = fileSystem.getPath(recoveredProject.originalProjectPath)

                    if(projectPath.exists()){
                        val project = PaperVisionProject.fromJson(String(fileSystem.readAllBytes(projectPath), StandardCharsets.UTF_8))

                        logger.info("Found recovered project ${recoveredProject.originalProjectPath} from ${recoveredProject.date} compared to ${project.timestamp}")

                        if(recoveredProject.date > project.timestamp) {
                            add(recoveredProject)
                            logger.info("Asking for recovered project ${recoveredProject.originalProjectPath} from ${recoveredProject.date}")
                        }
                    } else {
                        file.delete()
                        logger.info("Discarded phased recovery file for ${recoveredProject.originalProjectPath}")
                    }
                }
            }
        }
    }.toList()

    val onRefresh = EventHandler("PaperVisionProjectManager-onRefresh")

    fun init() {
        PaperVisionDaemon.attachToEditorChange {
            if(currentProject != null) {
                PaperVisionDaemon.invokeOnMainLoop {
                    sendRecoveryProject(currentProject!!)
                }
            }
        }
    }

    fun refresh() {
        projectTree = PaperVisionProjectTree(root)
        projects = recursiveSearchProjects(projectTree.rootTree)

        onRefresh.run()
    }

    private fun recursiveSearchProjects(root: PaperVisionProjectTree.ProjectTreeNode.Folder): List<PaperVisionProjectTree.ProjectTreeNode.Project> {
        val list = mutableListOf<PaperVisionProjectTree.ProjectTreeNode.Project>()

        for(node in root.nodes) {
            when(node) {
                is PaperVisionProjectTree.ProjectTreeNode.Folder -> {
                    list.addAll(recursiveSearchProjects(node))
                }
                is PaperVisionProjectTree.ProjectTreeNode.Project -> {
                    list.add(node)
                }
            }
        }

        return list
    }

    fun newProject(path: String, name: String, jsonElement: JsonElement? = null, appendExtension: Boolean = true) {
        val projectPath = fileSystem.getPath("/").resolve(path)
        fileSystem.createDirectories(projectPath)

        val projectFile = projectPath.resolve(if(appendExtension) "$name.paperproj" else name)
        fileSystem.createFile(projectFile)
        fileSystem.write(projectFile, PaperVisionProject(
            Instant.now().toEpochMilli(),
            path, name,
            jsonElement ?: JsonObject()
        ).toJson().toByteArray(StandardCharsets.UTF_8))

        refresh()
    }

    fun deleteProject(project: PaperVisionProjectTree.ProjectTreeNode.Project) {
        bulkDeleteProjects(project)
    }

    fun bulkDeleteProjects(vararg projects: PaperVisionProjectTree.ProjectTreeNode.Project) {
        for(project in projects) {
            val path = findProjectPath(project) ?: throw FileNotFoundException("Project $project not found in tree")
            fileSystem.delete(path)
        }

        refresh()
    }

    fun openProject(project: PaperVisionProjectTree.ProjectTreeNode.Project) {
        val path = findProjectPath(project) ?: throw FileNotFoundException("Project $project not found in tree")

        logger.info("Opening ${path.pathString}")

        PaperVisionDaemon.openProject(
            PaperVisionProject.fromJson(
                String(fileSystem.readAllBytes(path), StandardCharsets.UTF_8)
            ).json
        )

        currentProject = project
    }

    fun saveCurrentProject() {
        val project = currentProject ?: return
        val path = findProjectPath(project) ?: return

        logger.info("Saving ${path.pathString}")

        fileSystem.write(path, paperVisionProjectFrom(currentProject!!).toJson().toByteArray(StandardCharsets.UTF_8))
    }

    fun findProject(path: String, name: String): PaperVisionProjectTree.ProjectTreeNode.Project? {
        val paths = path.split("/").filter { it.isNotBlank() }

        // Start at the root of the project tree
        var currentNode: PaperVisionProjectTree.ProjectTreeNode = projectTree.rootTree

        if(path.isNotBlank() || path != "/") {
            // Traverse the path segments
            for (segment in paths) {
                if (currentNode is PaperVisionProjectTree.ProjectTreeNode.Folder) {
                    // Find the subfolder with the matching segment name
                    val nextNode = currentNode.nodes.find { it.name == segment }
                    if (nextNode != null) {
                        currentNode = nextNode
                    } else {
                        // If the segment is not found, return null
                        return null
                    }
                } else {
                    // If we encounter a non-folder node before the path is fully traversed, return null
                    return null
                }
            }
        }

        // At the end of the path, check for a project with the given name
        if (currentNode is PaperVisionProjectTree.ProjectTreeNode.Folder) {
            return currentNode.nodes.find {
                it is PaperVisionProjectTree.ProjectTreeNode.Project && it.name == name
            } as? PaperVisionProjectTree.ProjectTreeNode.Project
        }

        // If the final node is not a folder or doesn't contain the project, return null
        return null
    }

    fun findProjectFolderPath(project: PaperVisionProjectTree.ProjectTreeNode.Project) =
        findProjectPath(project)?.toAbsolutePath()?.parent

    fun findProjectPath(project: PaperVisionProjectTree.ProjectTreeNode.Project) = findProjectPath(project, projectTree.rootTree, root)

    private fun findProjectPath(
        targetProject: PaperVisionProjectTree.ProjectTreeNode.Project,
        currentNode: PaperVisionProjectTree.ProjectTreeNode,
        currentPath: Path
    ): Path? {
        return when (currentNode) {
            is PaperVisionProjectTree.ProjectTreeNode.Folder -> {
                for (node in currentNode.nodes) {
                    val path = findProjectPath(targetProject, node, currentPath.resolve(currentNode.name))
                    if (path != null) {
                        return path
                    }
                }
                null
            }
            is PaperVisionProjectTree.ProjectTreeNode.Project -> {
                if (currentNode == targetProject) {
                    return currentPath.resolve(currentNode.name)
                }
                null
            }
        }
    }

    fun sendRecoveryProject(projectNode: PaperVisionProjectTree.ProjectTreeNode.Project) {
        val projectPath = findProjectPath(projectNode)?.pathString ?: return
        val hex = projectPath.hexString

        logger.info("Sending recovery for $projectPath")

        recoveryDaemonProcessManager.sendRecoveryData(
            RecoveryData(
                recoveryFolder.path,
                "${hex}.recoverypaperproj",
                RecoveredProject(projectPath, System.currentTimeMillis(), hex, paperVisionProjectFrom(projectNode))
            )
        )
    }

    fun discardCurrentRecovery() {
        if(currentProject == null) return
        val path = findProjectPath(currentProject!!)?.pathString ?: return

        logger.info("Discarding recovery for $path")

        val recoveryFile = recoveryFolder.resolve("${path.hexString}.recoverypaperproj")
        recoveryFile.delete()
    }

    fun recoverProject(recoveredProject: RecoveredProject) {
        val projectPath = fileSystem.getPath(recoveredProject.originalProjectPath)

        if(projectPath.exists()) {
            fileSystem.delete(projectPath)
        }

        newProject(
            recoveredProject.project.path,
            recoveredProject.project.name,
            jsonElement = recoveredProject.project.json,
            appendExtension = false
        )

        logger.info("Recovered project ${recoveredProject.originalProjectPath} from ${recoveredProject.date}")

        refresh()
    }

    fun deleteAllRecoveredProjects() {
        for(file in recoveryFolder.listFiles() ?: arrayOf()) {
            if(file.extension == "recoverypaperproj") {
                file.delete()
            }
        }
    }

}