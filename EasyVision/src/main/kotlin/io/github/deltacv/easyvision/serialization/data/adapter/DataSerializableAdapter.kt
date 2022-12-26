package io.github.deltacv.easyvision.serialization.data.adapter

import com.google.gson.*
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import java.lang.reflect.Type

object DataSerializableAdapter : JsonSerializer<DataSerializable<*>>, JsonDeserializer<DataSerializable<*>> {

    override fun serialize(src: DataSerializable<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return dataSerializableToJsonObject(src, context)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): DataSerializable<*> {
        return jsonObjectToDataSerializable(json, context)
    }

}