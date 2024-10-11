package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.Type

object JavaTypes {

    val String = Type("String", "java.lang")

    fun ArrayList(elementType: Type) = Type(
        "ArrayList", "java.util",
        arrayOf(elementType)
    )

    val LabelAnnotation = Type("Label", "io.github.deltacv.eocvsim.virtualreflect.jvm")

}