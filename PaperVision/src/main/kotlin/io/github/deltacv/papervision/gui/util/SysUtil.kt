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