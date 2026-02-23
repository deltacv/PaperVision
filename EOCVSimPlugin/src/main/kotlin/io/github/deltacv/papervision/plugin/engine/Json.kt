package io.github.deltacv.papervision.plugin.engine

import io.github.deltacv.papervision.engine.client.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.client.response.PaperVisionEngineMessageResponse
import io.github.deltacv.papervision.serialization.generated.EOCVSimPluginPolymorphicSerializableMetadata
import io.github.deltacv.papervision.serialization.generated.SharedPolymorphicSerializableMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer

val ipcJson = Json {
    serializersModule = SerializersModule {
        polymorphic(PaperVisionEngineMessage::class) {
            SharedPolymorphicSerializableMetadata.registerPaperVisionEngineMessageSubclasses(this)
            EOCVSimPluginPolymorphicSerializableMetadata.registerPaperVisionEngineMessageSubclasses(this)
        }

        polymorphic(PaperVisionEngineMessageResponse::class) {
            SharedPolymorphicSerializableMetadata.registerPaperVisionEngineMessageResponseSubclasses(this)
            EOCVSimPluginPolymorphicSerializableMetadata.registerPaperVisionEngineMessageResponseSubclasses(this)
        }
    }
}