package io.github.deltacv.easyvision.serialization.data

interface DataSerializable<D: Any> {
    fun serialize(): D
    fun deserialize(data: D)
}