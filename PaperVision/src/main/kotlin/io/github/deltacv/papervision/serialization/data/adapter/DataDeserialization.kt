package io.github.deltacv.papervision.serialization.data.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.util.getFieldDeep
import io.github.deltacv.papervision.util.hasSuperclass

/**
 * Deserialize a json object to a DataSerializable object.
 */
@Suppress("UNCHECKED_CAST")
fun jsonObjectToDataSerializable(
    json: JsonElement,
    context: JsonDeserializationContext? = null,
    inst: DataSerializable<Any>? = null
): DataSerializable<*> {
    val classLoader = DataSerializable::class.java.classLoader
    val jsonObject = json.asJsonObject

    val dataClass = classLoader.loadClass(jsonObject.get("dataClass").asString)
    val dataObj = jsonObject.get("data") ?: JsonObject()
    val dataInstance = context?.deserialize(dataObj, dataClass) ?: gson.fromJson(dataObj, dataClass)

    val objectClass = classLoader.loadClass(jsonObject.get("objectClass").asString)

    val objectInstance = inst
        ?: try {
            objectClass.getConstructor(dataClass).newInstance(dataInstance)
        } catch (_: Exception) {
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

            // al chile no se
            if (field.type.hasSuperclass(DataSerializable::class.java)) {
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