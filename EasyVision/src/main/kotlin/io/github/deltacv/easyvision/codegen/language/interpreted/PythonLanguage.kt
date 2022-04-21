package io.github.deltacv.easyvision.codegen.language.interpreted

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.codegen.language.LanguageBase

object PythonLanguage : LanguageBase(
    usesSemicolon = false,
    genInClass = false,
    optimizeImports = false
) {

    override val Parameter.string get() = name

    override val trueValue = ConValue(BooleanType, "True")
    override val falseValue = ConValue(BooleanType, "False")

    override val newImportBuilder = { PythonImportBuilder(this) }

    override fun and(left: Condition, right: Condition) = condition("(${left.value}) and (${right.value})")
    override fun or(left: Condition, right: Condition) = condition("(${left.value}) or (${right.value})")

    override fun not(condition: Condition) = condition("not (${condition.value})")

    override fun instanceVariableDeclaration(
        vis: Visibility,
        variable: Variable,
        label: String?,
        isStatic: Boolean,
        isFinal: Boolean
    ) = Pair(
        if(label != null) {
            "label(\"$label\", \"${variable.name}\")"
        } else null,
        "${variable.name} = ${variable.variableValue.value}${semicolonIfNecessary()}"
    )

    override fun localVariableDeclaration(
        variable: Variable,
        isFinal: Boolean
    ) = instanceVariableDeclaration(Visibility.PUBLIC, variable).second

    override fun instanceVariableSetDeclaration(variable: Variable, v: Value) = "${variable.name} = ${v.value!!}" + semicolonIfNecessary()

    override fun streamMatCallDeclaration(id: Value, mat: Value, cvtColor: Value?) =
        if(cvtColor != null)
            methodCallDeclaration("stream", id, mat, cvtColor)
        else methodCallDeclaration("stream", id, mat)

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

    override fun ifStatementDeclaration(condition: Condition) = "if ${condition.value}"

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

    override fun new(type: Type, vararg parameters: Value) = ConValue(
        type, "${type.className}(${parameters.csv()})"
    )

    override fun nullVal(type: Type) = ConValue(type, "None")

    class PythonImportBuilder(val lang: Language) : Language.ImportBuilder {
        private val imports = mutableMapOf<String, MutableList<String>>()

        override fun import(type: Type) {
            if(lang.isImportExcluded(type)) return

            val classNames = imports[type.packagePath]

            if(classNames == null) {
                imports[type.packagePath] = mutableListOf(type.className)
            } else if(!classNames.contains(type.className)){
                classNames.add(type.className)
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