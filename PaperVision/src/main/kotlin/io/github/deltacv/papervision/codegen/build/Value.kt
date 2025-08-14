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

package io.github.deltacv.papervision.codegen.build

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.util.event.PaperVisionEventHandler
import io.github.deltacv.papervision.util.hexString

val String.v get() = ConValue(genType, this)
val Number.v get() = toString().v

abstract class Value {

    companion object {
        val NONE = ConValue(Type.NONE, null)
    }

    abstract val type: Type
    abstract val value: String?

    private val internalImports = mutableListOf<Type>()
    val imports get() = internalImports as List<Type>

    protected fun processImports() {
        // avoid adding imports for primitive types
        if(type.shouldImport) {
            additionalImports(type)
        }

        if(type.generics != null) {
            for(genericType in type.generics!!) {
                if(genericType.shouldImport) {
                    additionalImports(genericType)
                }
            }
        }
    }

    fun additionalImports(vararg imports: Type) {
        internalImports.addAll(imports)
    }

    fun additionalImports(vararg imports: Value) {
        for(import in imports) {
            internalImports.addAll(import.imports)
        }
    }
}

open class ConValue(override val type: Type, override val value: String?): Value() {
    init {
        processImports()
    }

    override fun toString() = value ?: "null"
}

class Condition(booleanType: Type, condition: String) : ConValue(booleanType, condition)
class Operation(numberType: Type, operation: String) : ConValue(numberType, operation)

open class Variable(val name: String, val variableValue: Value) : ConValue(variableValue.type, name) {

    constructor(type: Type, name: String) : this(name, ConValue(type, name))

    init {
        additionalImports(*variableValue.imports.toTypedArray())
    }

    override fun toString() = name
}