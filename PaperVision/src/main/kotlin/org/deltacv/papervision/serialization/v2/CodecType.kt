package org.deltacv.papervision.serialization.v2

@Target(AnnotationTarget.CLASS)
annotation class CodecType(
    val name: String = "",
    val instantiable: Boolean = true
)
