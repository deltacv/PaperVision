/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

inline fun <S: CodeGenSession> generatorsBuilder(init: GeneratorsContext<S>.() -> Unit) = GeneratorsContext<S>().run {
    init()
    generators
}