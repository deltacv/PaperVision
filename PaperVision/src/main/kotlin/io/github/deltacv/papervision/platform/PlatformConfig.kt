package io.github.deltacv.papervision.platform

import com.google.gson.GsonBuilder
import io.github.deltacv.papervision.util.loggerForThis
import java.io.File

class PaperVisionConfig {
    @JvmField
    var lang = "en"

    @JvmField
    var shouldAskForLang = true
}

abstract class PlatformConfig {
    var fields = PaperVisionConfig()

    abstract fun load()
    abstract fun save()
}

val defaultConfigPath = System.getProperty("user.home") + File.separator + ".papervision" + File.separator + "config.json"

class DefaultFilePlatformConfig : FilePlatformConfig(defaultConfigPath) {
    init {
        File(defaultConfigPath).parentFile.mkdirs()
    }
}

open class FilePlatformConfig(
    val path: String
) : PlatformConfig() {

    val gson = GsonBuilder().setPrettyPrinting().create()
    val file = File(path)

    val logger by loggerForThis()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            save()
        })
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