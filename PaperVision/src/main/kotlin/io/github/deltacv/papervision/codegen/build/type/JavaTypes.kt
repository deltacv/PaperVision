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

package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.Type

object JavaTypes {

    val String = Type("String", "java.lang")

    val Math = object: Type("Math", "java.lang") {
        override val shouldImport = false
    }

    fun ArrayList(elementType: Type) = Type(
        "ArrayList", "java.util",
        arrayOf(elementType)
    )

    fun HashMap(key: Type, value: Type) = Type(
        "HashMap", "java.util",
        arrayOf(key, value)
    )

    class Map(key: Type, value: Type) : Type(
        "Map", "java.util",
        arrayOf(key, value)
    ) {
        class Entry(key: Type, value: Type) : Type(
            "Entry", "java.util",
            arrayOf(key, value)
        )
    }

    fun List(elementType: Type) = Type(
        "List", "java.util",
        arrayOf(elementType)
    )

    val Collections = Type("Collections", "java.util")

    val LabelAnnotation = Type("Label", "io.github.deltacv.eocvsim.virtualreflect.jvm")

}