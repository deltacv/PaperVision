package io.github.deltacv.papervision.serialization.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.github.deltacv.papervision.serialization.data.adapter.DataSerializableAdapter
import io.github.deltacv.papervision.serialization.data.adapter.SerializeIgnoreExclusionStrategy

object DataSerializer {

    val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(DataSerializable::class.java, DataSerializableAdapter)
        .addSerializationExclusionStrategy(SerializeIgnoreExclusionStrategy)
        .create()

    val type = object : TypeToken<Map<String, List<DataSerializable<*>>>>() {}.type

    fun serialize(serializables: Map<String, List<DataSerializable<*>>>): String {
        return gson.toJson(serializables)
    }

    fun serializeToTree(serializables: Map<String, List<DataSerializable<*>>>): JsonElement {
        return gson.toJsonTree(serializables)
    }

    fun deserialize(data: String): Map<String, List<DataSerializable<*>>> {
        return gson.fromJson(data, type)
    }

    fun deserialize(obj: JsonElement): Map<String, List<DataSerializable<*>>> {
        return gson.fromJson(obj, type)
    }

}

@Target(AnnotationTarget.FIELD)
annotation class SerializeData

@Target(AnnotationTarget.CLASS)
annotation class SerializeIgnore

@Target(AnnotationTarget.CLASS)
annotation class SerializationName(val name: String)