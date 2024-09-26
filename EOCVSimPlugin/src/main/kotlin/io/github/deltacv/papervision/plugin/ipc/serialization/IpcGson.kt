package io.github.deltacv.papervision.plugin.ipc.serialization

import com.google.gson.GsonBuilder
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

val ipcGson get() = GsonBuilder()
    .registerTypeHierarchyAdapter(PaperVisionEngineMessage::class.java, IpcMessageAdapter)
    .registerTypeHierarchyAdapter(PaperVisionEngineMessageResponse::class.java, IpcMessageResponseAdapter)
    .registerTypeAdapter(Any::class.java, AnyAdapter)
    .create()