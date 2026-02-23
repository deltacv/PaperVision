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
package io.github.deltacv.papervision.plugin.project.recovery

import io.github.deltacv.papervision.plugin.project.EOCVSimPaperVisionProject
import kotlinx.serialization.Serializable

@Serializable
data class RecoveredProject(
    val originalProjectPath: String,
    val date: Long,
    val hash: String,
    val project: EOCVSimPaperVisionProject
)

@Serializable
data class RecoveryData(val recoveryFolderPath: String, val recoveryFileName: String, val projectData: RecoveredProject)