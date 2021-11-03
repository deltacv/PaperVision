package io.github.deltacv.easyvision.serialization.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.github.deltacv.easyvision.serialization.data.adapter.DataSerializableAdapter

object DataSerializer {

    val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(DataSerializable::class.java, DataSerializableAdapter)
        .create()

    val type = object : TypeToken<Map<String, List<DataSerializable<*>>>>() {}.type

    fun serialize(serializables: Map<String, List<DataSerializable<*>>>): String {
        return gson.toJson(serializables)
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