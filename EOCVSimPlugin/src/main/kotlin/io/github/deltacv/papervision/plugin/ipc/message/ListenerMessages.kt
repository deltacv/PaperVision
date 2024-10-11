package io.github.deltacv.papervision.plugin.ipc.message

import com.google.gson.JsonElement
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

class EditorChangeMessage(
    val json: JsonElement
) : PaperVisionEngineMessageBase()