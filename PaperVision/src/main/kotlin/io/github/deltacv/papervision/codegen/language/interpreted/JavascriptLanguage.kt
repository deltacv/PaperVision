package io.github.deltacv.papervision.codegen.language.interpreted

import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.*
import io.github.deltacv.papervision.codegen.csv
import io.github.deltacv.papervision.codegen.language.LanguageBase

object JavascriptLanguage : LanguageBase(genInClass = false, optimizeImports = false) {

    override val Parameter.string get() = name

    override fun instanceVariableDeclaration(
        vis: Visibility,
        variable: Variable,
        label: String?,
        isStatic: Boolean,
        isFinal: Boolean
    ) = Pair(
        if(label != null) {
            "label(\"${variable.name}\", \"$label\")"
        } else null,
        "var ${variable.name} = ${variable.variableValue.value}${semicolonIfNecessary()}"
    )

    override fun localVariableDeclaration(
        variable: Variable,
        isFinal: Boolean
    ) = instanceVariableDeclaration(Visibility.PUBLIC, variable).second

    override fun instanceVariableSetDeclaration(variable: Variable, v: Value) = "${variable.name} = ${v.value!!}${semicolonIfNecessary()}"

    override fun methodDeclaration(
        vis: Visibility,
        returnType: Type,
        name: String,
        vararg parameters: Parameter,
        isStatic: Boolean,
        isFinal: Boolean,
        isSynchronized: Boolean,
        isOverride: Boolean
    ): Pair<String?, String> {
        return Pair("",
            "function $name(${parameters.csv()})"
        )
    }

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for(var ${variable.value} in ${iterable.value})"

    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        vararg implements: Type,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        throw UnsupportedOperationException("Class declarations are not supported in JavaScript")
    }

    override fun enumClassDeclaration(name: String, vararg values: String): String {
        val builder = StringBuilder()

        for((i, value) in values.withIndex()) {
            builder.append("$value: $i")
            if(i < values.size - 1) {
                builder.append(",")
            }
            builder.appendLine()
        }

        return """var $name = {  
            |${builder.toString().trim()}
            |}""".trimMargin()
    }

    override fun importDeclaration(importPath: String, className: String) = "importClass($importPath.$className)${semicolonIfNecessary()}"

    override fun new(type: Type, vararg parameters: Value) = ConValue(
        type, "new ${type.className}(${parameters.csv()})"
    )

}