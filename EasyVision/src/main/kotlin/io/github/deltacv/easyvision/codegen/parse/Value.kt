package io.github.deltacv.easyvision.codegen.parse

import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.node.vision.Colors

fun new(type: String, vararg parameters: String) = Value(type, "new $type(${parameters.csv()})")

fun value(type: String, value: String) = Value(type, value)

fun callValue(methodName: String, returnType: String, vararg parameters: Value) =
    Value(returnType, "$methodName(${parameters.csv()})")

fun enumValue(type: String, constantName: String) = Value(type, "$type.$constantName")

fun cvtColorValue(a: Colors, b: Colors) = Value("int", "Imgproc.COLOR_${a.name}2${b.name}")

fun variable(type: String) = Value(type, null)

/**
 * Only for foreach
 */
fun variableName(type: String, name: String) = Value(type, name)

val String.v get() = Value("", this)

val Number.v get() = toString().v

data class Value(val type: String, val value: String?)