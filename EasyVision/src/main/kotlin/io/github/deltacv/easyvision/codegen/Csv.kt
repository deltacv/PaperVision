package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.Value

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

fun Array<out Value>.csv(): String {
    val stringArray = this.map { it.value!! }.toTypedArray()
    return stringArray.csv()
}

fun Array<out Type>.csv(): String {
    val stringArray = this.map { it.shortNameWithGenerics }.toTypedArray()
    return stringArray.csv()
}