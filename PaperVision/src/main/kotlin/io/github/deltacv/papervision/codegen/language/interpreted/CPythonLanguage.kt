/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen.language.interpreted

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.*
import io.github.deltacv.papervision.codegen.build.type.CPythonType
import io.github.deltacv.papervision.codegen.build.type.StandardTypes
import io.github.deltacv.papervision.codegen.csv
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.LanguageBase
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.loggerForThis
import kotlin.text.isNotBlank

object CPythonLanguage : LanguageBase(
    usesSemicolon = false
) {

    override val Parameter.string get() = name

    override val sourceFileExtension = "py"

    val NoType = object: CPythonType("None", "None") {
        override val shouldImport = false
    }

    override val IntType get() = NoType
    override val LongType get() = NoType
    override val FloatType get() = NoType
    override val DoubleType get() = NoType

    override val trueValue = ConValue(BooleanType, "True")
    override val falseValue = ConValue(BooleanType, "False")

    override val nullValue = ConValue(NoType, "None")

    override fun newImportBuilder() = PythonImportBuilder(this)

    fun valueIs(value: Value, type: Type) = condition("${value.value} is ${type.className}")
    fun valueIsNot(value: Value, type: Type) = condition("${value.value} is not ${type.className}")

    override fun and(left: Condition, right: Condition) = condition("(${left.value}) and (${right.value})")
    override fun or(left: Condition, right: Condition) = condition("(${left.value}) or (${right.value})")

    override fun not(condition: Condition) = condition("not (${condition.value})")

    override fun instanceVariableDeclaration(
        vis: Visibility,
        variable: DeclarableVariable,
        label: String?,
        isStatic: Boolean,
        isFinal: Boolean
    ) = Pair(
        null,
        "${variable.name} = ${variable.variableValue.value}${semicolonIfNecessary()}"
    )

    override fun localVariableDeclaration(
        variable: DeclarableVariable,
        isFinal: Boolean
    ) = instanceVariableDeclaration(Visibility.PUBLIC, variable).second

    override fun instanceVariableSetDeclaration(variable: DeclarableVariable, v: Value) = "${variable.name} = ${v.value!!}" + semicolonIfNecessary()

    override fun streamMatCallDeclaration(id: Value, mat: Value, cvtColor: Value) =
        throw UnsupportedOperationException("streamMatCallDeclaration is not supported in Python")

    override fun cvtColorValue(a: ColorSpace, b: ColorSpace): Value {
        var newA = a
        var newB = b

        if(a == ColorSpace.RGBA && b != ColorSpace.RGB) {
            newA = ColorSpace.RGB
        } else if(a != ColorSpace.RGB && b == ColorSpace.RGBA) {
            newB = ColorSpace.RGB
        }

        return ConValue(NoType, "cv2.COLOR_${newA.name}2${newB.name}")
    }

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
            "def $name(${parameters.csv()})"
        )
    }

    override fun ifStatementDeclaration(condition: Condition) = "if ${condition.value}"

    override fun forLoopDeclaration(variable: Value, start: Value, max: Value, step: Value?) =
        "for ${variable.value} in range(${start.value}, ${max.value}${step?.let { ", $it" } ?: ""})"

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for ${variable.value} in ${iterable.value}"

    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        vararg implements: Type,
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

    override fun castValue(value: Value, castTo: Type) = ConValue(castTo, value.value)

    fun sliceValue(start: Value? = null, end: Value? = null, step: Value? = null) : ConValue {
        val sliceStr = buildString {
            if(start != null) {
                append(start.value)
            }
            append(":")
            if(end != null) {
                append(end.value)
            }
            if(step != null) {
                append(":${step.value}")
            }
        }

        return ConValue(NoType, sliceStr)
    }

    override fun arrayOf(type: Type) = type

    override fun newArrayOf(type: Type, size: Value): ConValue {
        return ConValue(arrayOf(type), "[]")
    }

    override fun newArrayOf(type: Type, vararg values: Value): ConValue {
        return ConValue(arrayOf(type), "[${values.csv()}]")
    }

    override fun arraySize(array: Value) = ConValue(IntType, "len(${array.value})")

    override fun arrayValue(from: Value, index: Value, type: Type) = ConValue(
        type, "${from.value}[${index.value}]"
    )

    override fun block(start: String, body: Scope, indent: Int): String {
        val bodyStr = body.get().trimIndent().prependIndent("\t".repeat(indent + 1))
        val startIndent = "\t".repeat(indent)

        return "$startIndent${start.trim()}:\n$bodyStr"
    }

    override fun importDeclaration(importPath: String, className: String) =
        throw UnsupportedOperationException("importDeclaration(importPath, className) is not supported in Python")

    override fun new(type: Type, vararg parameters: Value) = ConValue(
        type, "${type.className}(${parameters.csv()})"
    )

    override fun nullVal(type: Type) = ConValue(type, "None")

    override fun gen(codeGen: CodeGen): String = codeGen.run {
        val mainScope = Scope(0, language, importScope)
        val classBodyScope = Scope(0, language, importScope)

        val start = classStartScope.get()
        if(start.isNotBlank()) {
            classBodyScope.scope(classStartScope, indentOverride = 0)
            classBodyScope.newStatement()
        }

        val init = initScope.get()
        if(init.isNotBlank()) {
            classBodyScope.scope(initScope, indentOverride = 0)
            classBodyScope.newStatement()
        }

        classBodyScope.method(
            Visibility.PUBLIC, NoType, "runPipeline", processFrameScope,
            Parameter(NoType, "input"), Parameter(NoType, "llrobot")
        )

        val end = classEndScope.get()
        if(end.isNotBlank()) {
            classBodyScope.scope(classEndScope)
        }

        mainScope.write(importScopePlaceholder.placeholder)
        mainScope.newStatement()

        mainScope.scope(classBodyScope, indentOverride = 0)

        mainScope.get()
    }

    class AccessorTupleVariable(vararg names: String) : AccessorVariable(NoType, "(${names.csv()})") {
        val names = names.toSet()

        fun get(name: String): Value {
            if(name !in names) {
                throw IllegalArgumentException("$name is not in the tuple variable")
            }

            return ConValue(NoType, name);
        }
    }

    class DeclarableTupleVariable(value: Value, vararg names: String) : DeclarableVariable(names.csv(), value) {
        val names = names.toSet()

        fun get(name: String): Value {
            if(name !in names) {
                throw IllegalArgumentException("$name is not in the tuple variable")
            }

            return ConValue(NoType, name);
        }
    }

    override fun comment(text: String): String {
        // handle single line and multi line
        return if(text.contains("\n")) {
            text.lines().joinToString("\n") { "# $it" }
        } else {
            "# $text"
        }
    }

    override fun int(value: Value) = callValue("int", language.IntType, value)
    override fun int(value: Int) = int(value.toString().v)

    override fun long(value: Value) = callValue("int", language.IntType, value)
    override fun long(value: Long) = long(value.toString().v)

    override fun float(value: Value) = callValue("float", language.IntType, value)
    override fun float(value: Float) = float(value.toString().v)

    override fun double(value: Value) = callValue("float", language.IntType, value)
    override fun double(value: Double) = double(value.toString().v)

    fun accessorTupleVariable(vararg names: String) = AccessorTupleVariable(*names)
    fun declaredTupleVariable(value: Value, vararg names: String) = DeclarableTupleVariable(value, *names)

    fun tuple(vararg value: Value) = ConValue(NoType, "(${value.csv()})")

    fun namedArgument(name: String, value: Value) = ConValue(NoType, "$name=${value.value}")

    class PythonImportBuilder(val lang: Language) : Language.ImportBuilder {
        private val imports = mutableMapOf<String, MutableSet<CPythonType>>() // Use MutableSet to avoid duplicates

        val logger by loggerForThis()

        override fun import(type: Type) {
            // Resolve actual import type
            val actualType = type.overridenImport ?: type

            // Skip excluded types or types that shouldn't be imported
            if (lang.isImportExcluded(actualType) || !actualType.shouldImport) return

            // Check if the actual type is a CPythonType, as that’s what we want to track
            if (actualType is CPythonType) {
                // Insert the type into the imports map under its package/module path
                imports.getOrPut(actualType.module) { mutableSetOf() }.add(actualType) // Add to MutableSet
            } else {
                logger.warn("Type $type is not a CPythonType and cannot be imported")
            }
        }

        override fun build(): String {
            val builder = StringBuilder()

            // Iterate over the modules and their types
            for ((module, types) in imports) {
                if (types.isEmpty()) continue

                // If only one type is imported, use the `import` syntax
                if (types.size == 1) {
                    builder.append("import ${types.first().module}")
                    if (types.first().alias != null) {
                        builder.append(" as ${types.first().alias}")
                    }
                    builder.appendLine()
                } else {
                    // Otherwise, use `from module import TypeA as AliasA, TypeB as AliasB`
                    builder.append("from $module import ")

                    // Handle aliases in the import statement
                    val typeNames = types.joinToString(", ") { type ->
                        if (type.alias != null) {
                            "${type.name} as ${type.alias}"  // Type with alias
                        } else {
                            type.name ?: type.module         // Type without alias
                        }
                    }
                    builder.append(typeNames).appendLine()
                }
            }

            return builder.toString().trim()
        }
    }

}
