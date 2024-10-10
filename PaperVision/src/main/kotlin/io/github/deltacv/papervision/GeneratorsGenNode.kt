package io.github.deltacv.papervision

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenNode
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

interface GeneratorsGenNode<S: CodeGenSession> : GenNode<S> {
    val generators: Map<Language, Generator<S>>

    fun map(language: Language, generator: Generator<S>) = Pair(language, generator)

    override fun genCode(current: CodeGen.Current): S {
        val generator = generators[current.language] ?: throw NoSuchElementException("No generator found for language ${current.language}")
        lastGenSession = generator.genCode(current)

        return lastGenSession!!
    }
}