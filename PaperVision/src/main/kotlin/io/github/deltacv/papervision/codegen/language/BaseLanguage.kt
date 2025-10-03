/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen.language

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.*
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.csv
import io.github.deltacv.papervision.util.toValidIdentifier

open class LanguageBase(
    val usesSemicolon: Boolean = true,
    val genInClass: Boolean = true,
    val optimizeImports: Boolean = true
) : Language {

    protected val mutableExcludedImports =  mutableListOf(
        JavaTypes.String
    )

    override val sourceFileExtension = "java"
    override val excludedImports get() = mutableExcludedImports.toList()

    override fun int(value: Value): Value {
        try {
            val i = value.value!!.toInt()
            return int(i)
        } catch (e: Exception) {
            return castValue(value, language.IntType)
        }
    }
    override fun long(value: Value): Value {
        try {
            val l = value.value!!.replace("l", "", ignoreCase = true).toLong()
            return long(l)
        } catch (e: NumberFormatException) {
            return castValue(value, language.LongType)
        }
    }
    override fun long(value: Long) = ConValue(LongType, "${value}L")

    override fun float(value: Value): Value {
        try {
            val f = value.value!!.replace("f", "", ignoreCase = true).toFloat()
            return float(f)
        } catch (e: NumberFormatException) {
            return castValue(value, language.FloatType)
        }
    }
    override fun float(value: Float) = ConValue(FloatType, "${value}f")

    override fun double(value: Value): Value {
        try {
            val d = value.value!!.replace("d", "", ignoreCase = true).toDouble()
            return double(d)
        } catch (e: NumberFormatException) {
            return castValue(value, language.DoubleType)
        }
    }
    override fun double(value: Double) = ConValue(DoubleType, value.toString())

    override fun nullVal(type: Type) = ConValue(type, "null")

    override fun arrayOf(type: Type): Type {
        var originalType = type

        while(originalType.overridenImport != null) {
            originalType = originalType.overridenImport!!
        }

        return Type("${type.className}[]", type.packagePath, type.generics, isArray = true, overridenImport = originalType)
    }

    override fun newArrayOf(type: Type, size: Value) = ConValue(
        arrayOf(type), "new ${type.className}${if(type.hasGenerics) "<>" else ""}[${size.value}]"
    )

    override fun newArrayOf(type: Type, vararg values: Value): Value {
        val arrayType = arrayOf(type)
        return ConValue(arrayType, "new ${type.className}${if(type.hasGenerics) "<>" else ""}[] { ${values.csv()} }")
    }

    override fun newImportBuilder(): Language.ImportBuilder = BaseImportBuilder(this)

    override val Parameter.string get() = "${type.shortNameWithGenerics} $name"

    override fun instanceVariableDeclaration(
        vis: Visibility,
        variable: Variable,
        label: String?,
        isStatic: Boolean,
        isFinal: Boolean
    ): Pair<String?, String> {
        val modifiers = if(isStatic) " static" else "" +
                if(isFinal) " final" else ""

        val ending = if(variable.variableValue.value != null) " = ${variable.variableValue.value}" else ""

        return Pair(
            if(label != null) {
                variable.additionalImports(JavaTypes.LabelAnnotation)
                "@Label(name = \"$label\")"
            } else null,
            "${vis.name.lowercase()}$modifiers ${variable.type.shortNameWithGenerics} ${variable.name}$ending${semicolonIfNecessary()}"
        )
    }

    override fun localVariableDeclaration(variable: Variable, isFinal: Boolean): String {
        val ending = (if(variable.variableValue.value != null) " = ${variable.variableValue.value}" else "")

        return "${if(isFinal) "final " else ""}${variable.type.shortNameWithGenerics} ${variable.name}$ending${semicolonIfNecessary()}"
    }

    override fun variableSetDeclaration(variable: Variable, v: Value) = "${variable.name} = ${v.value!!}${semicolonIfNecessary()}"

    override fun arrayVariableSetDeclaration(variable: Variable, index: Value, v: Value) =
        "${variable.name}[${index.value}] = ${v.value}${semicolonIfNecessary()}"

    override fun instanceVariableSetDeclaration(variable: Variable, v: Value) = "this.${variable.name} = ${v.value!!}${semicolonIfNecessary()}"

    override fun methodCallDeclaration(className: Type, methodName: String, vararg parameters: Value) =
        "${className.className}.$methodName(${parameters.csv()})${semicolonIfNecessary()}"

    override fun methodCallDeclaration(callee: Value, methodName: String, vararg parameters: Value) =
        "${callee.value}.$methodName(${parameters.csv()})${semicolonIfNecessary()}"

    override fun methodCallDeclaration(methodName: String, vararg parameters: Value) =
        "$methodName(${parameters.csv()})${semicolonIfNecessary()}"

    override fun streamMatCallDeclaration(id: Value, mat: Value, cvtColor: Value): String =
        if(cvtColor != Value.NONE)
            methodCallDeclaration("streamFrame", id, mat, cvtColor)
        else methodCallDeclaration("streamFrame", id, mat, nullValue)

    override fun constructorDeclaration(vis: Visibility, className: String, vararg parameters: Parameter) =
        "${vis.name.lowercase()} $className(${parameters.csv()})"

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
        val synchronized = if(isSynchronized) "synchronized " else ""

        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        return Pair(if(isOverride) {
            "@Override"
        } else null,
            "${vis.name.lowercase()} $synchronized$static$final${returnType.shortNameWithGenerics} $name(${parameters.csv()})"
        )
    }

    override fun returnDeclaration(value: Value?) =
        (if(value != null) {
            "return ${value.value!!}"
        } else "return") + semicolonIfNecessary()

    override fun ifStatementDeclaration(condition: Condition) = "if(${condition.value})"

    override fun forLoopDeclaration(variable: Value, start: Value, max: Value, step: Value?): String {
        val stepStr = if(step == null || step.value == "1") {
            "++"
        } else " += ${step.value}"

        return "for(${variable.type.shortNameWithGenerics} ${variable.value} = ${start.value} ; ${variable.value} < ${max.value} ; ${variable.value}$stepStr)"
    }

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for(${variable.type.shortNameWithGenerics} ${variable.value} : ${iterable.value})"
    override fun whileLoopDeclaration(condition: Condition) = "while(${condition.value})"

    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        vararg implements: Type,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        val e = if(extends != null) "extends ${extends.shortNameWithGenerics} " else ""
        val i = if(implements.isNotEmpty()) "implements ${implements.csv()} " else ""

        return "${vis.name.lowercase()} $static${final}class $name $e$i"
    }

    override fun enumClassDeclaration(name: String, vararg values: String) = "enum $name { ${values.csv() } "

    override fun block(start: String, body: Scope, tabs: String): String {
        val bodyStr = body.get()
        val endWhitespaceLine = if(!bodyStr.endsWith("\n")) "\n" else ""

        return "$tabs${start.trim()} {\n$bodyStr$endWhitespaceLine$tabs}"
    }

    open fun importDeclaration(importPath: String, className: String) = "import ${importPath}.${className}${semicolonIfNecessary()}"

    override fun string(value: String) = ConValue(JavaTypes.String, "\"$value\"")

    override fun string(value: Value) = string(value.value!!)

    override fun new(type: Type, vararg parameters: Value) = ConValue(
        type, "new ${type.className}${if(type.hasGenerics) "<>" else ""}(${parameters.csv()})"
    )

    override fun callValue(methodName: String, returnType: Type, vararg parameters: Value) = ConValue(
        returnType, "$methodName(${parameters.csv()})"
    ).apply {
        additionalImports(*parameters)
    }

    override fun callValue(classType: Type, methodName: String, returnType: Type, vararg parameters: Value) =
        ConValue(returnType, "${classType.className}.$methodName(${parameters.csv()})").apply {
            additionalImports(classType)
            additionalImports(*parameters)
        }

    override fun callValue(callee: Value, methodName: String, returnType: Type, vararg parameters: Value) =
        ConValue(returnType, "${callee.value}.$methodName(${parameters.csv()})").apply {
            additionalImports(callee, *parameters)
        }

    override fun propertyValue(from: Value, property: String, type: Type) = ConValue(type, "${from.value}.${property}")
    override fun propertyVariable(from: Value, property: String, type: Type) = Variable(type, "${from.value}.${property}")

    override fun arrayValue(from: Value, index: Value, type: Type) = ConValue(
        type, "${from.value}[${index.value}]"
    )

    override fun arraySize(array: Value) = ConValue(IntType, "${array.value}.length")
    override fun castValue(value: Value, castTo: Type) = ConValue(castTo, "((${castTo.shortNameWithGenerics}) (${value.value}))")

    override fun comment(text: String): String {
        if(text.contains('\n')) {
            return text.lines().joinToString("\n") { "// $it" }
        } else {
            return "// $text"
        }
    }

    override fun gen(codeGen: CodeGen): String = codeGen.run {
        val mainScope = Scope(0, language, importScope)
        val classBodyScope = Scope(1, language, importScope)

        val start = classStartScope.get()
        if(start.isNotBlank()) {
            classBodyScope.scope(classStartScope)
            classBodyScope.newStatement()
        }

        val init = initScope.get()
        if(init.isNotBlank()) {
            classBodyScope.method(
                Visibility.PUBLIC, language.VoidType, "init", initScope,
                Parameter(JvmOpenCvTypes.Mat, "input"), isOverride = true
            )
            classBodyScope.newStatement()
        }

        classBodyScope.method(
            Visibility.PUBLIC, JvmOpenCvTypes.Mat, "processFrame", processFrameScope,
            Parameter(JvmOpenCvTypes.Mat, "input"), isOverride = true
        )

        val viewportTapped = viewportTappedScope.get()
        if(viewportTapped.isNotBlank()) {
            classBodyScope.newStatement()

            classBodyScope.method(
                Visibility.PUBLIC, language.VoidType, "onViewportTapped", viewportTappedScope,
                isOverride = true
            )
        }

        val end = classEndScope.get()
        if(end.isNotBlank()) {
            classBodyScope.scope(classEndScope)
        }

        val pipelineClass = if(isForPreviz) JvmOpenCvTypes.StreamableOpenCvPipeline else JvmOpenCvTypes.OpenCvPipeline

        if(genInClass) {
            importScope.importType(pipelineClass)
        }

        mainScope.write(importScopePlaceholder.placeholder)
        mainScope.newStatement()

        if(genInClass) {
            mainScope.clazz(Visibility.PUBLIC, className.toValidIdentifier(), classBodyScope, extends = pipelineClass)
        } else {
            mainScope.scope(classBodyScope, trimIndent = true)
        }

        mainScope.get()
    }

    override val trueValue by lazy { ConValue(BooleanType, "true") }
    override val falseValue by lazy { ConValue(BooleanType, "false") }

    protected fun semicolonIfNecessary() = if(usesSemicolon) ";" else ""

    class BaseImportBuilder(val lang: LanguageBase) : Language.ImportBuilder {
        private val imports = mutableMapOf<String, MutableList<String>>()

        override fun import(type: Type) {
            val actualType = type.overridenImport ?: type

            if(lang.isImportExcluded(actualType)) return
            if(type.packagePath.isBlank()) return

            if(imports.containsKey(actualType.packagePath)) {
                val importsOfThis = imports[actualType.packagePath]!!

                if(importsOfThis.size > 2 && lang.optimizeImports) {
                    importsOfThis.clear()
                    importsOfThis.add("*")
                } else {
                    if(importsOfThis.size == 1 && importsOfThis[0] == "*") return

                    if(!importsOfThis.contains(actualType.className)) {
                        importsOfThis.add(actualType.className)
                    }
                }
            } else {
                imports[actualType.packagePath] = mutableListOf(actualType.className)
            }
        }

        override fun build(): String {
            val builder = StringBuilder()

            for((importPath, classNames) in imports) {
                for(className in classNames) {
                    builder.appendLine(lang.importDeclaration(importPath, className))
                }
            }

            return builder.toString().trim()
        }
    }
}