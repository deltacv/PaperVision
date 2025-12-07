package io.github.deltacv.papervision.codegen.language.vision

import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes

fun JvmOpenCvTypes.enableCircleType(): Type {

    return Type("Circle", "Circle")
}