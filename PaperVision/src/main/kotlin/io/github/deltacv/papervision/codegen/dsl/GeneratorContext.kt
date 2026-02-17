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

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

class GeneratorContext<I, S: CodeGenSession>(val genInput: I, val current: CodeGen.Current)

fun <I, S: CodeGenSession> generator(init: GeneratorContext<I, S>.() -> S) =
    Generator<I, S> { input, current -> init(GeneratorContext(input, current)) }

fun <I, S: CodeGenSession> generatorFor(language: Language, init: GeneratorContext<I, S>.() -> S) =
    language to generator(init)

fun <I, S: CodeGenSession> generatorFor(vararg languages: Language, init: GeneratorContext<I, S>.() -> S) =
    languages.associateWith { generator(init) }
