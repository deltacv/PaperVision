package io.github.deltacv.papervision.plugin.ipc.serialization

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import kotlin.jvm.java

private val gson = Gson()

open class PolymorphicAdapter<T>(
    val name: String,
    val classloader: ClassLoader = PolymorphicAdapter::class.java.classLoader
) : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.addProperty("${name}Class", src!!::class.java.name)
        obj.add(name, gson.toJsonTree(src))

        return obj
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        val className = json.asJsonObject.get("${name}Class").asString

        val clazz = classloader.loadClass(className)

        return gson.fromJson(json.asJsonObject.get(name), clazz) as T
    }

}