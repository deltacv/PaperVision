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
import io.github.deltacv.papervision.codegen.PolyglotMapping
import io.github.deltacv.papervision.codegen.language.Language
import kotlin.reflect.KClass

class GeneratorsBuilderCtx<I, S: CodeGenSession> {

    val generators = mutableMapOf<PolyglotMapping, Generator<I, S>>()

    /* ---------- INSTANCE LANGUAGES ---------- */

    fun generatorFor(vararg languages: Language, init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[PolyglotMapping.LanguagesInst(languages.toList())] = it
        }

    fun generatorFor(vararg languages: Language, generator: Generator<I, S>) =
        generators.put(PolyglotMapping.LanguagesInst(languages.toList()), generator)


    /* ---------- CLASS LANGUAGES ---------- */

    @JvmName("generatorForSingleClassLanguage")
    inline fun <reified L: Language> generatorFor(noinline init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[PolyglotMapping.LanguagesClass(listOf(L::class))] = it
        }

    inline fun <reified L: Language> generatorFor(generator: Generator<I, S>) =
        generators.put(
            PolyglotMapping.LanguagesClass(listOf(L::class)),
            generator
        )

    @JvmName("generatorForTwoClassLanguages")
    inline fun <reified L1: Language, reified L2: Language>
            generatorFor(noinline init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[
                PolyglotMapping.LanguagesClass(
                    listOf(L1::class, L2::class)
                )
            ] = it
        }

    /* ---------- CLASS LANGUAGES (VARARG) ---------- */

    fun generatorFor(
        vararg languageClasses: KClass<out Language>,
        init: GeneratorCtx<I, S>.() -> S
    ) =
        generator(init).also {
            generators[
                PolyglotMapping.LanguagesClass(languageClasses.toList())
            ] = it
        }

    fun generatorFor(
        vararg languageClasses: KClass<out Language>,
        generator: Generator<I, S>
    ) =
        generators.put(
            PolyglotMapping.LanguagesClass(languageClasses.toList()),
            generator
        )

    /* ---------- ANY ---------- */

    fun generatorForAny(init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[PolyglotMapping.AnyLanguage] = it
        }

    fun generatorForAny(generator: Generator<I, S>) =
        generators.put(PolyglotMapping.AnyLanguage, generator)
}

inline fun <I, S: CodeGenSession> generatorsBuilder(init: GeneratorsBuilderCtx<I, S>.() -> Unit) = GeneratorsBuilderCtx<I, S>().run {
    init()
    generators
}

@JvmName("generatorsBuilderUnit")
inline fun <S: CodeGenSession> generatorsBuilder(init: GeneratorsBuilderCtx<Unit, S>.() -> Unit) = generatorsBuilder<Unit, S>(init)