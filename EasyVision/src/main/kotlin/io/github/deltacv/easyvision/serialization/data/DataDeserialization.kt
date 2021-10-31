package io.github.deltacv.easyvision.serialization.data

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.github.deltacv.easyvision.node.hasSuperclass
import io.github.deltacv.easyvision.serialization.data.interfaces.DataSerializable
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun jsonObjectToDataSerializable(
    json: JsonElement,
    context: JsonDeserializationContext? = null,
    inst: DataSerializable<Any>? = null
): DataSerializable<*> {
    val jsonObject = json.asJsonObject

    val dataClass = Class.forName(jsonObject.get("dataClass").asString)
    val dataInstance = dataClass.getConstructor().newInstance()

    val dataObject = jsonObject.get("data")
    if(dataObject != null && dataObject.isJsonObject) {
        for(field in dataClass.declaredFields) {
            processField(field, dataInstance, dataObject.asJsonObject, context)
        }
    }

    val objectClass = Class.forName(jsonObject.get("objectClass").asString)

    val objectInstance = if(inst != null) inst else try {
        objectClass.getConstructor(dataClass).newInstance(dataInstance)
    } catch (e: Exception) {
        objectClass.getConstructor().newInstance()
    } as DataSerializable<Any>

    val objectObject = jsonObject.get("object")
    if(objectObject != null && objectObject.isJsonObject) {
        for(field in objectClass.declaredFields) {
            processField(field, objectInstance, objectObject.asJsonObject, context)
        }
    }
    objectInstance.deserialize(dataInstance)

    return objectInstance
}

private fun processField(field: Field, instance: Any, obj: JsonObject, context: JsonDeserializationContext? = null) {
    try {
        if(obj.has(field.name)) {
            val value = obj.get(field.name)
            field.isAccessible = true

            if (hasSuperclass(field.type, DataSerializable::class.java)) {
                val fieldValue = field.get(instance)
                if (fieldValue != null) {
                    jsonObjectToDataSerializable(value, context, fieldValue as DataSerializable<Any>)
                } else {
                    field.set(instance, jsonObjectToDataSerializable(value, context))
                }
            } else {
                field.set(
                    instance,
                    if (context != null) context.deserialize(value, field.type) else gson.fromJson(value, field.type)
                )
            }
        }
    } catch(e: Exception) {
        throw RuntimeException("Exception while processing field \"${field.name}\" of ${instance::class.java.typeName}", e)
    }
}