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

package io.github.deltacv.papervision.plugin.project

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import io.github.deltacv.papervision.engine.client.response.JsonElementResponse
import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.engine.client.response.StringResponse
import io.github.deltacv.papervision.plugin.PaperVisionProcessRunner
import io.github.deltacv.papervision.plugin.ipc.EOCVSimIpcEngine
import io.github.deltacv.papervision.plugin.ipc.message.DiscardCurrentRecoveryMessage
import io.github.deltacv.papervision.plugin.ipc.message.EditorChangeMessage
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentProjectMessage
import io.github.deltacv.papervision.plugin.ipc.message.SaveCurrentProjectMessage
import io.github.deltacv.papervision.plugin.project.recovery.RecoveredProject
import io.github.deltacv.papervision.plugin.project.recovery.RecoveryDaemonProcessManager
import io.github.deltacv.papervision.plugin.project.recovery.RecoveryData
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.hexString
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Instant
import javax.swing.SwingUtilities
import kotlin.io.path.exists
import kotlin.io.path.pathString

class PaperVisionProjectManager(
    val classpath: String,
    val fileSystem: SandboxFileSystem,
    val engine: EOCVSimIpcEngine,
    val eocvSim: EOCVSim
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

    var currentPaperVisionProject: PaperVisionProject? = null
        private set

    fun paperVisionProjectFrom(
        project: PaperVisionProjectTree.ProjectTreeNode.Project,
        tree: JsonElement
    ) = PaperVisionProject(
        Instant.now().toEpochMilli(),
        findProjectFolderPath(project)!!.pathString,
        project.name,
        tree
    )

    val recoveryDaemonProcessManager = RecoveryDaemonProcessManager(classpath).apply {
        start()
    }

    val logger by loggerForThis()

    val recoveredProjects get() = mutableListOf<RecoveredProject>().apply {
        if(recoveryFolder.exists()) {
            for(file in recoveryFolder.listFiles() ?: arrayOf()) {
                if(file.extension == "recoverypaperproj") {
                    val recoveredProject = try {
                        RecoveredProject.fromJson(file.readText())
                    } catch(e: Exception) {
                        logger.warn("Failed to read recovery file, deleting", e)
                        file.delete()
                        continue
                    }

                    val projectPath = fileSystem.getPath(recoveredProject.originalProjectPath)

                    if(projectPath.exists()){
                        val project = try {
                            PaperVisionProject.fromJson(String(fileSystem.readAllBytes(projectPath), StandardCharsets.UTF_8))
                        } catch(e: Exception) {
                            logger.warn("Failed to read project file for phased recovery, deleting", e)
                            file.delete()
                            continue
                        }

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

    val onRefresh = PaperVisionEventHandler("PaperVisionProjectManager-onRefresh")

    fun init() {
        engine.setMessageHandlerOf<DiscardCurrentRecoveryMessage> {
            discardCurrentRecovery()
            respond(OkResponse())
        }

        engine.setMessageHandlerOf<SaveCurrentProjectMessage> {
            saveCurrentProject(message.json)
            respond(OkResponse())
        }

        engine.setMessageHandlerOf<GetCurrentProjectMessage> {
            respond(JsonElementResponse(currentPaperVisionProject!!.json))
        }

        engine.setMessageHandlerOf<EditorChangeMessage> {
            if(currentProject != null) {
                sendRecoveryProject(currentProject!!, message.json)
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

        currentPaperVisionProject = PaperVisionProject.fromJson(
            String(fileSystem.readAllBytes(path), StandardCharsets.UTF_8)
        )

        SwingUtilities.invokeLater {
            eocvSim.visualizer.frame.isVisible = false
        }

        PaperVisionProcessRunner.onPaperVisionExit.doOnce {
            SwingUtilities.invokeLater {
                eocvSim.visualizer.frame.isVisible = true
            }
        }

        PaperVisionProcessRunner.execPaperVision(classpath)

        currentProject = project
    }

    fun saveCurrentProject(json: JsonElement) {
        val project = currentProject ?: return
        val path = findProjectPath(project) ?: return

        logger.info("Saving ${path.pathString}")

        fileSystem.write(path, paperVisionProjectFrom(currentProject!!, json).toJson().toByteArray(StandardCharsets.UTF_8))
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

    fun sendRecoveryProject(
        projectNode: PaperVisionProjectTree.ProjectTreeNode.Project,
        tree: JsonElement
    ) {
        val projectPath = findProjectPath(projectNode)?.pathString ?: return
        val hex = projectPath.hexString

        logger.debug("Sending recovery for {}", projectPath)

        recoveryDaemonProcessManager.sendRecoveryData(
            RecoveryData(
                recoveryFolder.path,
                "${hex}.recoverypaperproj",
                RecoveredProject(projectPath, System.currentTimeMillis(), hex, paperVisionProjectFrom(projectNode, tree))
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

    fun closeCurrentProject() {
        currentProject = null
    }

    fun deleteAllRecoveredProjects() {
        for(file in recoveryFolder.listFiles() ?: arrayOf()) {
            if(file.extension == "recoverypaperproj") {
                file.delete()
            }
        }
    }

}