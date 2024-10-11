package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.Type

open class CPythonType(
    val module: String,
    val name: String? = null,
    val alias: String? = null
) : Type(alias ?: name ?: module, module) {
    override val shouldImport = true
}