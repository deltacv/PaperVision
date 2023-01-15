package io.github.deltacv.easyvision.codegen.build

import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.node.vision.ColorSpace

interface ValueBuilder {

    val language: Language

    val trueValue: Value
    val falseValue: Value

    fun nullVal(type: Type): Value

    fun lessThan(left: Value, right: Value) = condition("${left.value} < ${right.value}")
    fun lessOrEqualThan(left: Value, right: Value) = condition("${left.value} <= ${right.value}")

    fun greaterThan(left: Value, right: Value) = condition("${left.value} > ${right.value}")
    fun greaterOrEqualThan(left: Value, right: Value) = condition("${left.value} >= ${right.value}")

    fun equals(left: Value, right: Value) = condition("${left.value} == ${right.value}")
    fun notEquals(left: Value, right: Value) = condition("${left.value} == ${right.value}")

    fun and(left: Condition, right: Condition) = condition("(${left.value}) && (${right.value})")
    fun or(left: Condition, right: Condition) = condition("(${left.value}) || (${right.value})")

    fun not(condition: Condition) = condition("!(${condition.value})")

    fun condition(value: String) = Condition(language.BooleanType, value)

    fun condition(value: Value) = Condition(language.BooleanType, value.value!!)

    fun sum(a: Value, b: Value) =
        operation(
            determineRelevantNumberType(a.type, b.type),
            "${a.toStringAndWrapIfOp()} + ${b.toStringAndWrapIfOp()}"
        )

    fun subtraction(a: Value, b: Value) =
        operation(
            determineRelevantNumberType(a.type, b.type),
            "${a.toStringAndWrapIfOp()} - ${b.toStringAndWrapIfOp()}"
        )

    fun multiplication(a: Value, b: Value) =
        operation(
            determineRelevantNumberType(a.type, b.type),
            "${a.toStringAndWrapIfOp()} * ${b.toStringAndWrapIfOp()}"
        )

    fun division(a: Value, b: Value) =
        operation(
            determineRelevantNumberType(a.type, b.type),
            "${a.toStringAndWrapIfOp()} / ${b.toStringAndWrapIfOp()}"
        )

    fun operation(relevantNumberType: Type, operation: String) = Operation(relevantNumberType, operation)

    fun determineRelevantNumberType(a: Type, b: Type) =
        if(a == language.DoubleType || b == language.DoubleType) {
            language.DoubleType
        } else if(a == language.FloatType || b == language.FloatType) {
            language.FloatType
        } else if(a == language.LongType || b == language.LongType) {
            language.LongType
        } else language.IntType

    fun Value.toStringAndWrapIfOp() = if(this is Operation) {
        "(${value})"
    } else value

    fun string(value: String): Value
    fun string(value: Value): Value

    fun new(type: Type, vararg parameters: Value): Value

    fun arrayOf(type: Type): Type

    fun newArrayOf(type: Type, size: Value): Value

    fun arraySize(array: Value): Value

    fun value(type: Type, value: String) = ConValue(type, value)

    fun callValue(methodName: String, returnType: Type, vararg parameters: Value): Value
    fun callValue(classType: Type, methodName: String, returnType: Type, vararg parameters: Value): Value
    fun callValue(callee: Value, methodName: String, returnType: Type, vararg parameters: Value): Value

    fun propertyValue(from: Value, property: String, type: Type): Value
    fun arrayValue(from: Value, index: Value, type: Type): Value

    fun enumValue(type: Type, constantName: String) = ConValue(type, "$type.$constantName")

    fun cvtColorValue(a: ColorSpace, b: ColorSpace): ConValue {
        var newA = a
        var newB = b

        if(a == ColorSpace.RGBA && b != ColorSpace.RGB) {
            newA = ColorSpace.RGB
        } else if(a != ColorSpace.RGB && b == ColorSpace.RGBA) {
            newB = ColorSpace.RGB
        }

        return ConValue(language.IntType, "Imgproc.COLOR_${newA.name}2${newB.name}").apply {
            additionalImports(OpenCvTypes.Imgproc)
        }
    }

    fun cvTypeValue(cvType: String): Value {
        return ConValue(language.IntType, "CvType.$cvType").apply {
            additionalImports(OpenCvTypes.CvType)
        }
    }

}