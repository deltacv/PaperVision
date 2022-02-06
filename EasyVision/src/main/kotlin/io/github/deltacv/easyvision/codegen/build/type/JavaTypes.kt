package io.github.deltacv.easyvision.codegen.build.type

import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.genType

object JavaTypes {

    val String = Type("String", "java.lang")

    fun ArrayList(elementType: Type) = Type(
        "ArrayList", "java.util",
        arrayOf(elementType)
    )

    val LabelAnnotation = Type("Label", "io.github.deltacv.eocvsim.virtualreflect.jvm")

}