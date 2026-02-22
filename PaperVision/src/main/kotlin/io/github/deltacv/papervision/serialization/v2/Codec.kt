package io.github.deltacv.papervision.serialization.v2

interface Codec<E> {
    fun encode(root: DataCodec): E
    fun decode(data: E, root: DataCodec): DataCodec
}