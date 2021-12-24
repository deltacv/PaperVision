package io.github.deltacv.easyvision.codegen.language

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.codegen.build.type.StandardTypes
import io.github.deltacv.easyvision.codegen.csv

interface Language : ValueBuilder {

    val excludedImports: List<String>

    override val language get() = this

    val IntType get() = StandardTypes.cint
    val LongType get() = StandardTypes.clong
    val FloatType get() = StandardTypes.cfloat
    val DoubleType get() = StandardTypes.cdouble

    val VoidType get() = StandardTypes.cvoid

    fun Array<out Parameter>.csv(): String {
        val stringArray = this.map { it.string }.toTypedArray()
        return stringArray.csv()
    }

    val Parameter.string: String

    fun isImportExcluded(import: String): Boolean {
        for(excludedImport in excludedImports) {
            if(excludedImport == import) {
                return true
            }
        }

        return false
    }

    fun instanceVariableDeclaration(
        vis: Visibility, name: String, variable: Value,
        isStatic: Boolean = false, isFinal: Boolean = false): String

    fun localVariableDeclaration(name: String, variable: Value, isFinal: Boolean = false): String

    fun variableSetDeclaration(name: String, v: Value): String
    fun instanceVariableSetDeclaration(name: String, v: Value): String

    fun methodCallDeclaration(className: Type, methodName: String, vararg parameters: Value): String
    fun methodCallDeclaration(methodName: String, vararg parameters: Value): String

    fun methodDeclaration(
        vis: Visibility, returnType: Type, name: String, vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = false
    ): Pair<String?, String>

    fun returnDeclaration(value: Value? = null): String

    fun foreachLoopDeclaration(variable: Value, iterable: Value): String
    fun whileLoopDeclaration(condition: Condition): String

    fun classDeclaration(vis: Visibility, name: String, body: Scope,
              extends: Type? = null, implements: Array<Type>? = null,
              isStatic: Boolean = false, isFinal: Boolean = false) : String

    fun enumClassDeclaration(name: String, vararg values: String): String

    fun importDeclaration(pkg: String): String

}