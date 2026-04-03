package org.deltacv.papervision.serialization.v2

interface Codec<E> {
    fun encode(root: DataCodec): E
    fun <C: DataCodec> decode(data: E, root: C): C
}



