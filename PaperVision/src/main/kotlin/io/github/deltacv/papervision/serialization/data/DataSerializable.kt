package io.github.deltacv.papervision.serialization.data

interface DataSerializable<D: Any> {
    fun serialize(): D
    fun deserialize(data: D)
}