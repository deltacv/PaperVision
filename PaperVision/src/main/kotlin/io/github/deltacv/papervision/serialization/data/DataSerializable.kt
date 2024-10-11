package io.github.deltacv.papervision.serialization.data

interface DataSerializable<D: Any> {
    val shouldSerialize: Boolean
        get() = true

    fun serialize(): D
    fun deserialize(data: D)
}