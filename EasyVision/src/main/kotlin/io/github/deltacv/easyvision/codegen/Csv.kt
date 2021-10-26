package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.parse.Parameter
import io.github.deltacv.easyvision.codegen.parse.Value

fun Array<out String>.csv(): String {
    val builder = StringBuilder()

    for((i, parameter) in this.withIndex()) {
        builder.append(parameter)

        if(i < this.size - 1) {
            builder.append(", ")
        }
    }

    return builder.toString()
}

fun Array<out Parameter>.csv(): String {
    val stringArray = this.map { "${it.type} ${it.name}" }.toTypedArray()
    return stringArray.csv()
}

fun Array<out Value>.csv(): String {
    val stringArray = this.map { it.value!! }.toTypedArray()
    return stringArray.csv()
}