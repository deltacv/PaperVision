package io.github.deltacv.easyvision.serialization.ev.adapter

import com.google.gson.*
import imgui.ImVec2
import java.lang.reflect.Type

object ImVec2Adapter : JsonSerializer<ImVec2>, JsonDeserializer<ImVec2> {
    override fun serialize(src: ImVec2, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val array = JsonArray()
        array.add(src.x)
        array.add(src.y)
        return array
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ImVec2 {
        TODO("Not yet implemented")
    }
}