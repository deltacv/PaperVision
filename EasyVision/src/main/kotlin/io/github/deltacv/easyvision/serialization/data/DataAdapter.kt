package io.github.deltacv.easyvision.serialization.data

import com.google.gson.*
import io.github.deltacv.easyvision.node.hasSuperclass
import java.lang.reflect.Type

object DataSerializableAdapter : JsonSerializer<DataSerializable<*>>, JsonDeserializer<DataSerializable<*>> {

    override fun serialize(src: DataSerializable<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val data = src.serialize()

        val dataObject = JsonObject()
        dataObject.addProperty("dataClass", data::class.java.name)
        dataObject.add("data", context.serialize(data))

        dataObject.addProperty("objectClass", src::class.java.name)
        dataObject.add("object", src.toJsonObject())

        return dataObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DataSerializable<*> {
        TODO("Implement")
    }

}

private val gson = GsonBuilder()
        .registerTypeAdapter(DataSerializable::class.java, DataSerializableAdapter)
        .create()

fun DataSerializable<*>.toJsonObject(): JsonObject {
    val obj = JsonObject()

    for(field in this::class.java.declaredFields) {
        field.isAccessible = true
        val value = field.get(this)

        if(hasSuperclass(field.type, DataSerializable::class.java)) {
            value as DataSerializable<*>
            val data = value.serialize()
            val dataObject = JsonObject()

            dataObject.addProperty("dataClass", data::class.java.name)
            dataObject.add("data", gson.toJsonTree(data))

            dataObject.addProperty("objectClass", field.type.name)
            dataObject.add("object", value.toJsonObject())

            obj.add(field.name, dataObject)
        } else if(field.isAnnotationPresent(SerializeData::class.java)) {
            obj.add(field.name, gson.toJsonTree(value))
        }
    }

    return obj
}