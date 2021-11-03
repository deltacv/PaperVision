package io.github.deltacv.easyvision.serialization.data.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.deltacv.easyvision.node.hasSuperclass
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun jsonObjectToDataSerializable(
    json: JsonElement,
    context: JsonDeserializationContext? = null,
    inst: DataSerializable<Any>? = null
): DataSerializable<*> {
    val jsonObject = json.asJsonObject

    val dataClass = Class.forName(jsonObject.get("dataClass").asString)
    val dataObj = jsonObject.get("data") ?: JsonObject()
    val dataInstance = context?.deserialize(dataObj, dataClass) ?: gson.fromJson(dataObj, dataClass)

    val objectClass = Class.forName(jsonObject.get("objectClass").asString)

    val objectInstance = inst
        ?: try {
            objectClass.getConstructor(dataClass).newInstance(dataInstance)
        } catch (e: Exception) {
            objectClass.getConstructor().newInstance()
        } as DataSerializable<Any>

    val objectObject = jsonObject.get("object")
    if(objectObject != null && objectObject.isJsonObject) {
        for((name, obj) in objectObject.asJsonObject.entrySet()) {
            processValue(objectInstance, name, obj, context)
        }
    }
    objectInstance.deserialize(dataInstance)

    return objectInstance
}

@Suppress("UNCHECKED_CAST")
private fun processValue(instance: Any, valueName: String, value: JsonElement, context: JsonDeserializationContext? = null) {
    try {
        val field = instance::class.java.getFieldDeep(valueName)

        if(field != null) {
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
                    if (context != null)
                        context.deserialize(value, field.type)
                    else gson.fromJson(value, field.type)
                )
            }
        }
    } catch(e: Exception) {
        throw RuntimeException("Exception while processing json entry \"$valueName\" of ${instance::class.java.typeName}", e)
    }
}

fun Class<*>.getFieldDeep(name: String): Field? {
    var field: Field? = null
    var clazz: Class<*>? = this

    while (clazz != null && field == null) {
        try {
            field = clazz.getDeclaredField(name)
        } catch (ignored: Exception) { }
        clazz = clazz.superclass
    }

    return field
}