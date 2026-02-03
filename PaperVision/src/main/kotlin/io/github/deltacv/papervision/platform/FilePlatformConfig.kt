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

package io.github.deltacv.papervision.platform

import com.google.gson.GsonBuilder
import io.github.deltacv.papervision.util.loggerForThis
import java.io.File
import kotlin.getValue

open class FilePlatformConfig(
    val path: String
) : PlatformConfig() {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    val file = File(path)

    val logger by loggerForThis()

    init {
        Runtime.getRuntime().addShutdownHook(Thread({
            save()
        }, "PlatformConfig-ShutdownHook"))
    }

    override fun load() {
        if(!file.exists()) {
            save()
            return
        }

        logger.info("Loading config from $path")
        fields = gson.fromJson(file.readText(), PaperVisionConfig::class.java)
    }

    override fun save() {
        logger.info("Saving config to $path")
        file.writeText(gson.toJson(fields))
    }
}

val defaultConfigPath = System.getProperty("user.home") + File.separator + ".papervision" + File.separator + "config.json"

class DefaultFilePlatformConfig : FilePlatformConfig(defaultConfigPath) {
    init {
        File(defaultConfigPath).parentFile.mkdirs() // mkdir .papervision
    }
}
