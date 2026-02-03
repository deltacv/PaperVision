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

package io.github.deltacv.papervision.gui.util

import java.lang.management.ManagementFactory

enum class OperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN
}

val OS: OperatingSystem get() {
    val osName = System.getProperty("os.name").lowercase()

    return when {
        osName.contains("win") -> OperatingSystem.WINDOWS
        osName.contains("nux") -> OperatingSystem.LINUX
        osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
        else -> OperatingSystem.UNKNOWN
    }
}

enum class SystemArchitecture {
    X86,
    X86_64,
    ARMv7,
    ARMv8,
    UNKNOWN
}

val ARCH: SystemArchitecture get() {
    val arch = System.getProperty("os.arch")

    return when {
        arch.contains("amd64") || arch.contains("x86_64") -> SystemArchitecture.X86_64
        arch.contains("x86") || arch.contains("i38") -> SystemArchitecture.X86
        arch.contains("arm64") || arch.contains("aarch") -> SystemArchitecture.ARMv8
        arch.contains("arm") -> SystemArchitecture.ARMv7
        else -> SystemArchitecture.UNKNOWN
    }
}

fun getMemoryUsageMB(): Long {
    val mb = 1024 * 1024
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb
}

fun getProcessCPULoad(): Double {
    val osBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    return osBean.processCpuLoad
}
