package io.github.deltacv.easyvision.serialization.data.adapter

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import io.github.deltacv.easyvision.node.hasSuperclass
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.data.SerializeData

fun DataSerializable<*>.toJsonObject(): JsonObject {
    val obj = JsonObject()

    for(field in this::class.java.declaredFields) {
        field.isAccessible = true
        val value = field.get(this)

        if(hasSuperclass(field.type, DataSerializable::class.java)) {
            obj.add(field.name, dataSerializableToJsonObject(value as DataSerializable<*>))
        } else if(field.isAnnotationPresent(SerializeData::class.java)) {
            obj.add(field.name, gson.toJsonTree(value))
        }
    }

    return obj
}

fun dataSerializableToJsonObject(value: DataSerializable<*>, context: JsonSerializationContext? = null): JsonElement {
    val data = value.serialize()
    val dataObject = JsonObject()

    dataObject.addProperty("dataClass", data::class.java.name)

    val jsonData = if(context != null) context.serialize(data) else gson.toJsonTree(data)
    if(jsonData.isJsonObject) {
        if((jsonData as JsonObject).size() > 0) {
            dataObject.add("data", jsonData)
        }
    } else {
        dataObject.add("data", jsonData)
    }

    dataObject.addProperty("objectClass", value::class.java.name)

    val jsonObject = value.toJsonObject()
    if(jsonObject.size() > 0) {
        dataObject.add("object", jsonObject)
    }

    return dataObject
}
