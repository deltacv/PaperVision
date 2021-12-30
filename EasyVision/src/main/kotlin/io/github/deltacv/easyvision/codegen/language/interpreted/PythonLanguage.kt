package io.github.deltacv.easyvision.codegen.language.interpreted

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Scope
import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.LanguageBase

object PythonLanguage : LanguageBase(
    usesSemicolon = false,
    genInClass = false,
    optimizeImports = false
) {

    override val Parameter.string get() = name

    override val newImportBuilder = { PythonImportBuilder() }

    override fun instanceVariableDeclaration(
        vis: Visibility,
        name: String,
        variable: Value,
        isStatic: Boolean,
        isFinal: Boolean
    ) = "$name = ${variable.value}" + semicolonIfNecessary()

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
            "def $name(${parameters.csv()})"
        )
    }

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for ${variable.value} in ${iterable.value}"

    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        implements: Array<Type>?,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        throw UnsupportedOperationException("Class declarations are not supported in Python")
    }

    override fun enumClassDeclaration(name: String, vararg values: String): String {
        val builder = StringBuilder()

        for(value in values) {
            builder.append("$value: \"$value\"").appendLine()
        }

        return """var $name = {  
            |${builder.toString().trim()}
            |}""".trimMargin()
    }

    override fun block(start: String, body: Scope, tabs: String): String {
        val bodyStr = body.get()

        return "$tabs${start.trim()}:\n$bodyStr"
    }

    override fun importDeclaration(importPath: String, className: String) =
        throw UnsupportedOperationException("importDeclaration(importPath, className) is not supported in Python")

    override fun new(type: Type, vararg parameters: Value) = Value(
        type, "${type.shortName}(${parameters.csv()})"
    )

    class PythonImportBuilder : Language.ImportBuilder {
        private val imports = mutableMapOf<String, MutableList<String>>()

        override fun import(type: Type) {
            val classNames = imports[type.importName]

            if(classNames == null) {
                imports[type.importName] = mutableListOf(type.shortName)
            } else if(!classNames.contains(type.shortName)){
                classNames.add(type.shortName)
            }
        }

        override fun build(): String {
            val builder = StringBuilder()

            for((importPath, classNames) in imports) {
                builder.appendLine().append("from $importPath import ")

                if(classNames.size > 1) builder.append("(")

                for((i, className) in classNames.withIndex()) {
                    builder.append(className)

                    if(i != classNames.size -1) builder.append(", ")
                }

                if(classNames.size > 1) builder.append(")")
            }

            return builder.toString().trim()
        }

    }

}