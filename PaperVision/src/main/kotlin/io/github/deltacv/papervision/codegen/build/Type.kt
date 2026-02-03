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

package io.github.deltacv.papervision.codegen.build

import io.github.deltacv.papervision.codegen.csv

open class Type(
    val className: String,
    open val packagePath: String = "",
    val generics: Array<Type>? = null,

    open val overridenImport: Type? = null,
    val isArray: Boolean = false
) {

    companion object {
        val NONE = Type("", "")
    }

    val hasGenerics get() = !generics.isNullOrEmpty()

    open val shouldImport get() = className != packagePath

    val shortNameWithGenerics get() =
        if(generics != null)
            "$className<${generics.csv()}>"
        else className

    override fun toString() = "Type(className=$className, packagePath=$packagePath, actualImport=$overridenImport, isArray=$isArray)"

}

private val typeCache = mutableMapOf<Class<*>, Type>()

val Any.genType: Type get() = this::class.java.genType

val Class<*>.genType: Type get() {
    if(!typeCache.containsKey(this)) {
        typeCache[this] = Type(simpleName, getPackage()?.name ?: "")
    }

    return typeCache[this]!!
}
