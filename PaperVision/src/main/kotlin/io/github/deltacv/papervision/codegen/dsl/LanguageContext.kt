package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.build.*
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.node.vision.ColorSpace

open class LanguageContext(val language: Language) {

    val Int.v get() = ConValue(language.IntType, toString())
    val Long.v get() = ConValue(language.LongType, toString())
    val Float.v get() = ConValue(language.FloatType, toString())
    val Double.v get() = ConValue(language.DoubleType, toString())

    val trueValue get() = language.trueValue
    val falseValue get() = language.falseValue

    val BooleanType get() = language.BooleanType
    val IntType get() = language.IntType
    val LongType get() = language.LongType
    val FloatType get() = language.FloatType
    val DoubleType get() = language.DoubleType

    val VoidType get() = language.VoidType

    val Type.nullVal get() = language.nullVal(this)
    val Value.nullVal get() = type.nullVal

    infix fun Value.lessThan(right: Value) = language.lessThan(this, right)
    infix fun Value.lessOrEqualThan(right: Value) = language.lessOrEqualThan(this, right)

    infix fun Value.greaterThan(right: Value) = language.greaterThan(this, right)
    infix fun Value.greaterOrEqualThan(right: Value) = language.greaterOrEqualThan(this, right)

    infix fun Value.equalsTo(right: Value) = language.equals(this, right)

    infix fun Condition.and(right: Condition) = language.and(this, right)
    infix fun Condition.or(right: Condition) = language.or(this, right)

    fun Value.condition(): Condition {
        require(type == language.BooleanType) {
            "Cannot convert value of type $type to condition (needs the Language's BooleanType)"
        }

        return language.condition(this)
    }

    // NUMBER OPERATORS

    operator fun Value.plus(right: Value) = language.sum(this, right)
    operator fun Value.minus(right: Value) = language.subtraction(this, right)
    operator fun Value.times(right: Value) = language.multiplication(this, right)
    operator fun Value.div(right: Value) = language.division(this, right)

    infix fun Int.plus(right: Int) = language.sum(this.v, right.v)
    infix fun Long.plus(right: Long) = language.sum(this.v, right.v)
    infix fun Float.plus(right: Float) = language.sum(this.v, right.v)
    infix fun Double.plus(right: Double) = language.sum(this.v, right.v)

    infix fun Int.minus(right: Int) = language.subtraction(this.v, right.v)
    infix fun Long.minus(right: Long) = language.subtraction(this.v, right.v)
    infix fun Float.minus(right: Float) = language.subtraction(this.v, right.v)
    infix fun Double.minus(right: Double) = language.subtraction(this.v, right.v)

    infix fun Int.by(right: Int) = language.multiplication(this.v, right.v)
    infix fun Long.by(right: Long) = language.multiplication(this.v, right.v)
    infix fun Float.by(right: Float) = language.multiplication(this.v, right.v)
    infix fun Double.by(right: Double) = language.multiplication(this.v, right.v)

    infix fun Int.between(right: Int) = language.division(this.v, right.v)
    infix fun Long.between(right: Long) = language.division(this.v, right.v)
    infix fun Float.between(right: Float) = language.division(this.v, right.v)
    infix fun Double.between(right: Double) = language.division(this.v, right.v)

    fun int(value: Value) = language.int(value)
    fun int(value: Int) = language.int(value)

    fun long(value: Value) = language.long(value)
    fun long(value: Long) = language.long(value)

    fun float(value: Value) = language.float(value)
    fun float(value: Float) = language.float(value)

    fun double(value: Value) = language.double(value)
    fun double(value: Double) = language.double(value)

    fun new(type: Type, vararg parameters: Value) = language.new(type, *parameters)

    fun string(value: String) = language.string(value)
    fun string(value: Value) = language.string(value)

    @JvmName("newExt")
    fun Type.new(vararg parameters: Value) = new(this, *parameters)

    fun Type.arrayType() = language.arrayOf(this)

    fun Value.arraySize() = language.arraySize(this)

    fun Type.newArray(size: Value) = language.newArrayOf(this, size)

    fun value(type: Type, value: String) = language.value(type, value)

    fun String.callValue(returnType: Type, vararg parameters: Value) =
        language.callValue(this, returnType, *parameters)

    fun Type.callValue(methodName: String, returnType: Type, vararg parameters: Value) =
        language.callValue(this, methodName, returnType, *parameters)

    fun Value.callValue(methodName: String, returnType: Type, vararg parameters: Value) =
        language.callValue(this, methodName, returnType, *parameters)

    fun Value.propertyValue(property: String, type: Type) = language.propertyValue(this, property, type)
    fun Value.propertyVariable(property: String, type: Type) = language.propertyVariable(this, property, type)

    operator fun Value.get(index: Value, type: Type) = language.arrayValue(this, index, type)

    fun enumValue(type: Type, constantName: String) = language.enumValue(type, constantName)

    fun cvtColorValue(a: ColorSpace, b: ColorSpace) = language.cvtColorValue(a, b)

    fun cvTypeValue(cvType: String) = language.cvTypeValue(cvType)

    fun variable(name: String, value: Value) = Variable(name, value)
    fun variable(type: Type, name: String) = Variable(type, name)
}