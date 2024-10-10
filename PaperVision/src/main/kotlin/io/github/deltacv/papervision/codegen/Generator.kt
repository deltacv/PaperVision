package io.github.deltacv.papervision.codegen

interface Generator<S: CodeGenSession> {
    fun genCode(current: CodeGen.Current): S
}