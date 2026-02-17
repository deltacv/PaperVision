package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.language.Language

interface PolyglotGenerator<I, S: CodeGenSession> : Generator<I, S> {
    val generators: Map<Language, Generator<I, S>>

    override fun genCode(input: I, current: CodeGen.Current): S {
        val generator = generators[current.language]
            ?: throw NoSuchElementException("No generator found for language ${current.language.javaClass.simpleName} at node ${this::class.simpleName}")

        return generator.genCode(input, current)
    }
}