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