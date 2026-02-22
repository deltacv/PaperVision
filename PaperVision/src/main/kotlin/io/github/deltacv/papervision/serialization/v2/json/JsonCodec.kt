package io.github.deltacv.papervision.serialization.v2.json

import io.github.deltacv.papervision.serialization.v2.Codec
import io.github.deltacv.papervision.serialization.v2.DataCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class JsonCodec(
    private val prettyPrint: Boolean = false
) : Codec<String> {

    private val json = Json { prettyPrint = this@JsonCodec.prettyPrint }

    override fun encode(root: DataCodec): String {
        val writer = JsonDataWriter()
        root.encode(writer)

        val jsonObject = writer.build()
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    override fun decode(data: String, root: DataCodec): DataCodec {
        val jsonObject: JsonObject = json.decodeFromString(JsonObject.serializer(), data)
        val reader = JsonDataReader(jsonObject)

        root.decode(reader)
        return root
    }

}