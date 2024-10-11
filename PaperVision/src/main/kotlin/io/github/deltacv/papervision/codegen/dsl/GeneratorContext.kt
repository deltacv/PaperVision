package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

class GeneratorContext<S: CodeGenSession>(val current: CodeGen.Current)

fun <S: CodeGenSession> generator(init: GeneratorContext<S>.() -> S) =
    object: Generator<S> {
        override fun genCode(current: CodeGen.Current) = init(GeneratorContext(current))
    }

fun <S: CodeGenSession> generatorFor(language: Language, init: GeneratorContext<S>.() -> S) =
    language to generator(init)

fun <S: CodeGenSession> generatorFor(vararg languages: Language, init: GeneratorContext<S>.() -> S) =
    languages.map { it to generator(init) }.toMap()