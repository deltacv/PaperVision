package io.github.deltacv.papervision.plugin.ipc.message

import com.google.gson.JsonElement
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

class GetCurrentProjectMessage : PaperVisionEngineMessageBase()

class DiscardCurrentRecoveryMessage : PaperVisionEngineMessageBase()

class SaveCurrentProjectMessage(
    var json: JsonElement
) : PaperVisionEngineMessageBase()