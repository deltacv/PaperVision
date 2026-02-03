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

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.language.Language

class GeneratorContext<S: CodeGenSession>(val current: CodeGen.Current)

fun <S: CodeGenSession> generator(init: GeneratorContext<S>.() -> S) =
    Generator { current -> init(GeneratorContext(current)) }

fun <S: CodeGenSession> generatorFor(language: Language, init: GeneratorContext<S>.() -> S) =
    language to generator(init)

fun <S: CodeGenSession> generatorFor(vararg languages: Language, init: GeneratorContext<S>.() -> S) =
    languages.associateWith { generator(init) }
