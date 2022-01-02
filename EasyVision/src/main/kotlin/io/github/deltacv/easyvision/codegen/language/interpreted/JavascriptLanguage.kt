package io.github.deltacv.easyvision.codegen.language.interpreted

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Scope
import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.codegen.language.LanguageBase

object JavascriptLanguage : LanguageBase(genInClass = false, optimizeImports = false) {

    override val Parameter.string get() = name

    override fun instanceVariableDeclaration(
        vis: Visibility,
        name: String,
        variable: Value,
        isStatic: Boolean,
        isFinal: Boolean
    ) = "var $name = ${variable.value}" + semicolonIfNecessary()

    override fun localVariableDeclaration(
        name: String,
        variable: Value,
        isFinal: Boolean
    ) = instanceVariableDeclaration(Visibility.PUBLIC, name, variable)

    override fun instanceVariableSetDeclaration(name: String, v: Value) = "$name = ${v.value!!}" + semicolonIfNecessary()

    override fun methodDeclaration(
        vis: Visibility,
        returnType: Type,
        name: String,
        vararg parameters: Parameter,
        isStatic: Boolean,
        isFinal: Boolean,
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
        implements: Array<Type>?,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        throw UnsupportedOperationException("Class declarations are not supported in JavaScript")
    }

    override fun enumClassDeclaration(name: String, vararg values: String): String {
        val builder = StringBuilder()

        for((i, value) in values.withIndex()) {
            builder.append("$value: $i").appendLine()
        }

        return """var $name = {  
            |${builder.toString().trim()}
            |}""".trimMargin()
    }

    override fun importDeclaration(importPath: String, className: String) = "importClass($importPath.$className)" + semicolonIfNecessary()

    override fun new(type: Type, vararg parameters: Value) = Value(
        type, "new ${type.className}(${parameters.csv()})"
    )

}