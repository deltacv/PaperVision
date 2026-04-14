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

package org.deltacv.papervision.codegen.dsl

import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.Generator
import org.deltacv.papervision.codegen.PolyglotMapping
import org.deltacv.papervision.codegen.language.Language
import kotlin.reflect.KClass

class PolyglotGeneratorBuilderCtx<I, S: CodeGenSession> {

    val generators = mutableMapOf<PolyglotMapping, Generator<I, S>>()

    /* ---------- INSTANCE LANGUAGES ---------- */

    fun generatorFor(vararg languages: Language, init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[PolyglotMapping.LanguageInsts(languages.toList())] = it
        }

    fun generatorFor(vararg languages: Language, generator: Generator<I, S>) =
        generators.put(PolyglotMapping.LanguageInsts(languages.toList()), generator)


    /* ---------- CLASS LANGUAGES ---------- */

    @JvmName("generatorForSingleClassLanguage")
    inline fun <reified L: Language> generatorFor(noinline init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[PolyglotMapping.LanguageClasses(listOf(L::class))] = it
        }

    inline fun <reified L: Language> generatorFor(generator: Generator<I, S>) =
        generators.put(
            PolyglotMapping.LanguageClasses(listOf(L::class)),
            generator
        )

    @JvmName("generatorForTwoClassLanguages")
    inline fun <reified L1: Language, reified L2: Language>
            generatorFor(noinline init: GeneratorCtx<I, S>.() -> S) =
        generator(init).also {
            generators[
                PolyglotMapping.LanguageClasses(
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
                PolyglotMapping.LanguageClasses(languageClasses.toList())
            ] = it
        }

    fun generatorFor(
        vararg languageClasses: KClass<out Language>,
        generator: Generator<I, S>
    ) =
        generators.put(
            PolyglotMapping.LanguageClasses(languageClasses.toList()),
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

inline fun <I, S: CodeGenSession> polyglot(init: PolyglotGeneratorBuilderCtx<I, S>.() -> Unit) = PolyglotGeneratorBuilderCtx<I, S>().run {
    init()
    generators
}

@JvmName("polyglotUnit")
inline fun <S: CodeGenSession> polyglot(init: PolyglotGeneratorBuilderCtx<Unit, S>.() -> Unit) = polyglot<Unit, S>(init)