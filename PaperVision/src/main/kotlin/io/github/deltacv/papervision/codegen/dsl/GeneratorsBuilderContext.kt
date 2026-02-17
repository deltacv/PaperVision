/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

class GeneratorsBuilderContext<I, S: CodeGenSession> {
    val generators = mutableMapOf<Language, Generator<I, S>>()

    fun generatorFor(language: Language, init: GeneratorContext<I, S>.() -> S) = generatorFor<I, S>(language, init).apply { generators[first] = second }
    fun generatorFor(vararg languages: Language, init: GeneratorContext<I, S>.() -> S) = generatorFor<I, S>(*languages, init = init).apply { forEach { generators[it.key] = it.value } }

    fun generatorFor(language: Language, generator: Generator<I, S>) = generators.put(language, generator)
    fun generatorFor(vararg languages: Language, generator: Generator<I, S>) = languages.forEach { generators[it] = generator }
}

inline fun <I, S: CodeGenSession> generatorsBuilder(init: GeneratorsBuilderContext<I, S>.() -> Unit) = GeneratorsBuilderContext<I, S>().run {
    init()
    generators
}

@JvmName("generatorsBuilderUnit")
inline fun <S: CodeGenSession> generatorsBuilder(init: GeneratorsBuilderContext<Unit, S>.() -> Unit) = generatorsBuilder<Unit, S>(init)