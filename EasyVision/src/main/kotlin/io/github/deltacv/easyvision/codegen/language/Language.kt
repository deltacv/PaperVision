package io.github.deltacv.easyvision.codegen.language

import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.codegen.build.type.StandardTypes
import io.github.deltacv.easyvision.codegen.csv

interface Language : ValueBuilder {

    val excludedImports: List<Type>

    override val language get() = this

    val BooleanType get() = StandardTypes.cboolean

    val IntType get() = StandardTypes.cint
    val LongType get() = StandardTypes.clong
    val FloatType get() = StandardTypes.cfloat
    val DoubleType get() = StandardTypes.cdouble

    val VoidType get() = StandardTypes.cvoid

    val nullValue get() = ConValue(VoidType, "null")

    val newImportBuilder: () -> ImportBuilder

    fun Array<out Parameter>.csv(): String {
        val stringArray = this.map { it.string }.toTypedArray()
        return stringArray.csv()
    }

    val Parameter.string: String

    fun isImportExcluded(import: Type) = excludedImports.contains(import)


    fun int(value: Value) = castValue(value, language.IntType)
    fun int(value: Int) = ConValue(language.IntType, value.toString())

    fun long(value: Value) = castValue(value, language.LongType)
    fun long(value: Long) = ConValue(LongType, value.toString())

    fun float(value: Value) = castValue(value, language.FloatType)
    fun float(value: Float) = ConValue(FloatType, value.toString())

    fun double(value: Value) = castValue(value, language.DoubleType)
    fun double(value: Double) = ConValue(DoubleType, value.toString())

    fun instanceVariableDeclaration(
        vis: Visibility, variable: Variable, label: String? = null,
        isStatic: Boolean = false, isFinal: Boolean = false): Pair<String?, String>

    fun localVariableDeclaration(variable: Variable, isFinal: Boolean = false): String

    fun variableSetDeclaration(variable: Variable, v: Value): String
    fun arrayVariableSetDeclaration(variable: Variable, index: Value, v: Value): String
    fun instanceVariableSetDeclaration(variable: Variable, v: Value): String
    fun methodCallDeclaration(className: Type, methodName: String, vararg parameters: Value): String
    fun methodCallDeclaration(callee: Value, methodName: String, vararg parameters: Value): String

    fun methodCallDeclaration(methodName: String, vararg parameters: Value): String

    fun streamMatCallDeclaration(id: Value, mat: Value, cvtColor: Value? = null): String

    fun constructorDeclaration(vis: Visibility, className: String, vararg parameters: Parameter): String

    fun methodDeclaration(
        vis: Visibility, returnType: Type, name: String, vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isSynchronized: Boolean = false, isOverride: Boolean = false
    ): Pair<String?, String>

    fun returnDeclaration(value: Value? = null): String

    fun ifStatementDeclaration(condition: Condition): String

    fun forLoopDeclaration(variable: Value, start: Value, max: Value, step: Value?): String
    fun foreachLoopDeclaration(variable: Value, iterable: Value): String
    fun whileLoopDeclaration(condition: Condition): String

    fun classDeclaration(vis: Visibility, name: String, body: Scope,
              extends: Type? = null, vararg implements: Type,
              isStatic: Boolean = false, isFinal: Boolean = false) : String

    fun enumClassDeclaration(name: String, vararg values: String): String

    fun block(start: String, body: Scope, tabs: String): String

    fun gen(codeGen: CodeGen): String
    interface ImportBuilder {
        fun import(type: Type)

        fun build(): String
    }

}