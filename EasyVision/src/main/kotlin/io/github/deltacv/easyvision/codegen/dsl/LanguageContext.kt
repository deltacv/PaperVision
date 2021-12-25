package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.codegen.build.Condition
import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.codegen.build.genType
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.node.vision.Colors

open class LanguageContext(val language: Language) {

    infix fun Value.lessThan(right: Value) = language.lessThan(this, right)
    infix fun Value.lessOrEqualThan(right: Value) = language.lessOrEqualThan(this, right)

    infix fun Value.greaterThan(right: Value) = language.greaterThan(this, right)
    infix fun Value.greaterOrEqualThan(right: Value) = language.greaterOrEqualThan(this, right)

    infix fun Value.equalsTo(right: Value) = language.equals(this, right)

    infix fun Condition.and(right: Condition) = language.and(this, right)
    infix fun Condition.or(right: Condition) = language.or(this, right)

    fun new(type: Type, vararg parameters: Value) = language.new(type, *parameters)

    @JvmName("newExt")
    fun Type.new(vararg parameters: Value) = new(this, *parameters)

    fun value(type: Type, value: String) = language.value(type, value)

    fun callValue(methodName: String, returnType: Type, vararg parameters: Value) =
        language.callValue(methodName, returnType, *parameters)

    fun callValue(classType: Type, methodName: String, returnType: Type, vararg parameters: Value) =
        language.callValue("${classType.shortName}.$methodName", returnType, *parameters).apply {
            additionalImport(classType.importName)
        }

    fun enumValue(type: Type, constantName: String) = language.enumValue(type, constantName)

    fun cvtColorValue(a: Colors, b: Colors) = language.cvtColorValue(a, b)

    fun variable(type: Type) = language.variable(type)

    fun variableName(type: Type, name: String) = language.variableName(type, name)

}