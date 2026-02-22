package io.github.deltacv.papervision.serialization.v2

interface DataCodec {
    fun encode(encoder: DataWriter)
    fun decode(decoder: DataReader)
}