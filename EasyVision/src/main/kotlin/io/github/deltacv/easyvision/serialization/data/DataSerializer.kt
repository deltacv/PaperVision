package io.github.deltacv.easyvision.serialization.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.github.deltacv.easyvision.serialization.data.interfaces.DataSerializable

object DataSerializer {

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(DataSerializable::class.java, DataSerializableAdapter)
        .create()

    fun serialize(serializables: Map<String, List<DataSerializable<*>>>): String {
        return gson.toJson(serializables)
    }

    fun deserialize(data: String): Map<String, DataSerializable<*>> {
        return gson.fromJson(data, object : TypeToken<Map<String, List<DataSerializable<*>>>>() {}.type)
    }

}

@Target(AnnotationTarget.FIELD)
annotation class SerializeData