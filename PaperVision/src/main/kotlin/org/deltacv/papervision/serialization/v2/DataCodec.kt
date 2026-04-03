package org.deltacv.papervision.serialization.v2

interface DataCodec {
    fun encode(encoder: DataEncoder)
    fun decode(decoder: DataDecoder)
}



