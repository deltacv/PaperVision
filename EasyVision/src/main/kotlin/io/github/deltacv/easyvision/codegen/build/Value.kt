package io.github.deltacv.easyvision.codegen.build

val String.v get() = ConValue(genType, this)
val Number.v get() = toString().v

abstract class Value {

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
}

class Condition(booleanType: Type, condition: String) : ConValue(booleanType, condition)
class Operation(numberType: Type, operation: String) : ConValue(numberType, operation)

class Variable(val name: String, val variableValue: Value) : ConValue(variableValue.type, name) {

    constructor(type: Type, name: String) : this(name, ConValue(type, name))

    init {
        additionalImports(*variableValue.imports.toTypedArray())
    }

}