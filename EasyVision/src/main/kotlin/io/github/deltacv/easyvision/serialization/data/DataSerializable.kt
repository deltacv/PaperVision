package io.github.deltacv.easyvision.serialization.data

interface DataSerializable<D: Any> {
    fun makeSerializationData(): D
    fun takeDeserializationData(data: D)

    fun serialize(): D
    fun deserialize(data: D)
}