package io.github.deltacv.papervision.codegen

inline fun <reified T> T.resolved() = Resolvable.Now(this)