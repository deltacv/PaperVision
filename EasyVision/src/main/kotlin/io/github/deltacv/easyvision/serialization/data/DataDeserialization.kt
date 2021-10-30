package io.github.deltacv.easyvision.serialization.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val deserializerGson = Gson()

private val typetoken = object: TypeToken<Map<String, List<ObjectEntry>>>() {}

fun deserializeObjectEntries(json: String)/*: Map<String, List<DataSerializable<*>>>*/ {
    val result = mutableMapOf<String, MutableList<ObjectEntry>>()
    val entries: Map<String, List<ObjectEntry>> = deserializerGson.fromJson(json, typetoken.type)

    for((name, entries) in entries) {
        result[name] = mutableListOf()

        for(entry in entries) {
            println("${entry.dataClass} ${entry.objectClass}")
        }
    }
}

data class ObjectEntry(val dataClass: String, val data: Map<String, Any>,
                       val objectClass: String, val obj: Map<String, Any>)