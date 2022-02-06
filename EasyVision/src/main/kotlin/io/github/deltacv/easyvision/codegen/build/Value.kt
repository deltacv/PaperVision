package io.github.deltacv.easyvision.codegen.build

val String.v get() = Value(genType, this)
val Number.v get() = toString().v

open class Value(val type: Type, val value: String?) {

    private val internalImports = mutableListOf<Type>()
    val imports get() = internalImports as List<Type>

    init {
        // avoid adding imports for primitive types
        if(type.shouldImport) {
            additionalImports(type)
        }

        if(type.generics != null) {
            for(genericType in type.generics) {
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

class Condition(booleanType: Type, condition: String) : Value(booleanType, condition)

class Variable(val name: String, val variableValue: Value) : Value(variableValue.type, name) {

    constructor(type: Type, name: String) : this(name, Value(type, name))

    init {
        additionalImports(*variableValue.imports.toTypedArray())
    }

}