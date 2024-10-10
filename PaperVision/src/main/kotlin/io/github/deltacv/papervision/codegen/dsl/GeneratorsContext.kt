package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

class GeneratorsContext<S: CodeGenSession> {
    val generators = mutableMapOf<Language, Generator<S>>()

    fun generatorFor(language: Language, init: GeneratorContext<S>.() -> S) = generatorFor<S>(language, init).apply { generators[first] = second }
    fun generatorFor(vararg languages: Language, init: GeneratorContext<S>.() -> S) = generatorFor<S>(*languages, init = init).apply { forEach { generators[it.key] = it.value } }

    fun generatorFor(language: Language, generator: Generator<S>) = generators.put(language, generator)
    fun generatorFor(vararg languages: Language, generator: Generator<S>) = languages.forEach { generators[it] = generator }
}

fun <S: CodeGenSession> generators(init: GeneratorsContext<S>.() -> Unit) = GeneratorsContext<S>().run {
    init()
    generators
}