package io.github.deltacv.papervision.plugin.ipc.serialization

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

object IpcMessageAdapter : PolymorphicAdapter<PaperVisionEngineMessage>("message")
object IpcMessageResponseAdapter : PolymorphicAdapter<PaperVisionEngineMessageResponse>("messageResponse")

object AnyAdapter : PolymorphicAdapter<Any>("mack")