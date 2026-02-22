package io.github.deltacv.papervision.serialization.v2.json

import kotlinx.serialization.json.*
import io.github.deltacv.papervision.serialization.v2.*

class JsonDataWriter : DataWriter {

    private val map = mutableMapOf<String, JsonElement>()

    fun build(): JsonObject = JsonObject(map)

    override var isIgnored = false
        private set

    override fun int(key: String, value: Int) {
        map[key] = JsonPrimitive(value)
    }

    override fun float(key: String, value: Float) {
        map[key] = JsonPrimitive(value)
    }

    override fun double(key: String, value: Double) {
        map[key] = JsonPrimitive(value)
    }

    override fun bool(key: String, value: Boolean) {
        map[key] = JsonPrimitive(value)
    }

    override fun string(key: String, value: String) {
        map[key] = JsonPrimitive(value)
    }

    override fun obj(key: String, value: DataCodec, typeName: String?) {
        val writer = JsonDataWriter()
        value.encode(writer)

        if(writer.isIgnored) {
            return // object requested for us to ignore it
        }

        val type = typeName // override typeName with passed parameter first
            ?: CodecTypeRegistry.nameOf(value::class) // look up in the register if no parameter was passed
            ?: throw NoSuchElementException("Unregistered codec: ${value::class}") // oops

        map[key] = buildJsonObject {
            put("_type", type)
            put("_data", writer.build())
        }
    }

    // ---- Typed lists ----

    override fun intList(key: String, values: List<Int>) {
        map[key] = JsonArray(values.map { JsonPrimitive(it) })
    }

    override fun floatList(key: String, values: List<Float>) {
        map[key] = JsonArray(values.map { JsonPrimitive(it) })
    }

    override fun doubleList(key: String, values: List<Double>) {
        map[key] = JsonArray(values.map { JsonPrimitive(it) })
    }

    override fun stringList(key: String, values: List<String>) {
        map[key] = JsonArray(values.map { JsonPrimitive(it) })
    }

    override fun boolList(key: String, values: List<Boolean>) {
        map[key] = JsonArray(values.map { JsonPrimitive(it) })
    }

    override fun objList(key: String, values: List<DataCodec>) {
        val array = JsonArray(buildList {
            for (value in values) {
                val writer = JsonDataWriter()
                value.encode(writer)

                if (writer.isIgnored) continue

                val type = CodecTypeRegistry.nameOf(value::class)
                    ?: throw NoSuchElementException("Unregistered DataCodec: ${value::class.qualifiedName}")

                add(buildJsonObject {
                    put("_type", type)
                    put("_data", writer.build())
                })
            }
        })

        map[key] = array
    }

    override fun ignore() {
        isIgnored = true
    }

    override fun unignore() {
        isIgnored = false
    }
}