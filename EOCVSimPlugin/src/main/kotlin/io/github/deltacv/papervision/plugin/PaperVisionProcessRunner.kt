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

package io.github.deltacv.papervision.plugin

import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.JavaProcess.SLF4JIOReceiver
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.papervision.plugin.ipc.EOCVSimIpcEngine
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Manages and executes the PaperVision process in a dedicated daemon thread.
 *
 * This object is responsible for starting PaperVision within a static daemon thread, allowing it
 * to run in the background without blocking the main thread. The daemon thread will remain active
 * as long as PaperVision is running and will exit when PaperVision stops or the application shuts down.
 *
 * It is expected that the PaperVision process will be initialized and controlled through this object.
 */
object PaperVisionProcessRunner {

    val logger by loggerForThis()

    val paperVisionEngine = EOCVSimIpcEngine()

    val onPaperVisionExit = PaperVisionEventHandler("PaperVisionProcessRunner-OnPaperVisionExit")

    val onPaperVisionExitError = PaperVisionEventHandler("PaperVisionProcessRunner-OnPaperVisionExitError")

    private val pool = Executors.newFixedThreadPool(1)

    var isRunning = false
        private set

    private var currentJob: Future<*>? = null

    fun execPaperVision(classpath: String) {
        if(isRunning) return

        isRunning = true

        currentJob = pool.submit {
            logger.info("Starting PaperVision process...")

            val programParams = listOf("-q", "-i=${paperVisionEngine.server.port}")

            println("classpath: $classpath")

            val exitCode = if(SysUtil.OS == SysUtil.OperatingSystem.MACOS) {
                logger.info("Running on macOS, adding platform-specific flags")
                JavaProcess.execClasspath(
                    EOCVSimIpcPaperVisionMain::class.java,
                    SLF4JIOReceiver(logger),
                    classpath,
                    listOf("-XstartOnFirstThread", "-Djava.awt.headless=true"),
                    programParams
                )
            } else {
                JavaProcess.execClasspath(
                    EOCVSimIpcPaperVisionMain::class.java,
                    SLF4JIOReceiver(logger),
                    classpath,
                    null,
                    programParams,
                )
            }

            onPaperVisionExit.run()

            if(exitCode != 0) {
                onPaperVisionExitError.run()
            }

            logger.warn("PaperVision process has exited with exit code $exitCode")

            isRunning = false
        }
    }

    fun stopPaperVision() {
        if(!isRunning) return

        currentJob?.cancel(true)
        isRunning = false
        currentJob = null
    }

}