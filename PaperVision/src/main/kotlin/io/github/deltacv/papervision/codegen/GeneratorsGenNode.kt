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

package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.language.Language

interface GeneratorsGenNode<S: CodeGenSession> : GenNode<S> {
    val generators: Map<Language, Generator<S>>

    fun map(language: Language, generator: Generator<S>) = Pair(language, generator)

    override fun genCode(current: CodeGen.Current): S {
        val generator = generators[current.language] ?: throw NoSuchElementException("No generator found for language ${current.language.javaClass.simpleName} at node ${this::class.simpleName}")
        lastGenSession = generator.genCode(current)

        return lastGenSession!!
    }
}