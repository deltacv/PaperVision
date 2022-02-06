package io.github.deltacv.easyvision.codegen.build

import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.node.vision.Colors

interface ValueBuilder {

    val language: Language

    fun lessThan(left: Value, right: Value) = condition("${left.value} < ${right.value}")
    fun lessOrEqualThan(left: Value, right: Value) = condition("${left.value} <= ${right.value}")

    fun greaterThan(left: Value, right: Value) = condition("${left.value} > ${right.value}")
    fun greaterOrEqualThan(left: Value, right: Value) = condition("${left.value} >= ${right.value}")

    fun equals(left: Value, right: Value) = condition("${left.value} == ${right.value}")

    fun and(left: Condition, right: Condition) = condition("(${left.value}) && (${right.value})")
    fun or(left: Condition, right: Condition) = condition("(${left.value}) || (${right.value})")

    fun condition(value: String) = Condition(language.BooleanType, value)

    fun new(type: Type, vararg parameters: Value): Value

    fun value(type: Type, value: String) = Value(type, value)

    fun callValue(methodName: String, returnType: Type, vararg parameters: Value): Value
    fun callValue(classType: Type, methodName: String, returnType: Type, vararg parameters: Value): Value
    fun callValue(callee: Value, methodName: String, returnType: Type, vararg parameters: Value): Value

    fun enumValue(type: Type, constantName: String) = Value(type, "$type.$constantName")

    fun cvtColorValue(a: Colors, b: Colors): Value {
        var newA = a
        var newB = b

        if(a == Colors.RGBA && b != Colors.RGB) {
            newA = Colors.RGB
        } else if(a != Colors.RGB && b == Colors.RGBA) {
            newB = Colors.RGB
        }

        return Value(language.IntType, "Imgproc.COLOR_${newA.name}2${newB.name}").apply {
            additionalImports(OpenCvTypes.Imgproc)
        }
    }

}