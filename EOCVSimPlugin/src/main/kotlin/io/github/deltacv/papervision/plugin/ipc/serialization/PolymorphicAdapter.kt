package io.github.deltacv.papervision.plugin.ipc.serialization

import com.google.gson.*
import java.lang.reflect.Type

private val gson = Gson()

open class PolymorphicAdapter<T>(val name: String) : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.addProperty("${name}Class", src!!::class.java.name)
        obj.add(name, gson.toJsonTree(src))

        return obj
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        val className = json.asJsonObject.get("${name}Class").asString
        val clazz = Class.forName(className)

        return gson.fromJson(json.asJsonObject.get(name), clazz) as T
    }

}