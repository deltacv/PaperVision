package io.github.deltacv.easyvision.codegen.language

import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.csv

open class LanguageBase(
    val usesSemicolon: Boolean = true,
    val genInClass: Boolean = true,
    val optimizeImports: Boolean = true
) : Language {

    protected val mutableExcludedImports =  mutableListOf(
        JavaTypes.String
    )

    override val excludedImports = mutableExcludedImports as List<Type>

    override val newImportBuilder: () -> Language.ImportBuilder = { BaseImportBuilder(this) }

    override val Parameter.string get() = "${type.shortNameWithGenerics} $name"

    override fun instanceVariableDeclaration(
        vis: Visibility,
        name: String,
        variable: Value,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        val modifiers = if(isStatic) " static" else "" +
                if(isFinal) " final" else ""

        val ending = if(variable.value != null) "= ${variable.value};" else ";"

        return "${vis.name.lowercase()}$modifiers ${variable.type.shortNameWithGenerics} $name $ending"
    }

    override fun localVariableDeclaration(name: String, variable: Value, isFinal: Boolean): String {
        val ending = (if(variable.value != null) "= ${variable.value}" else "") + semicolonIfNecessary()

        return "${if(isFinal) "final " else ""}${variable.type.shortNameWithGenerics} $name $ending"
    }

    override fun variableSetDeclaration(name: String, v: Value) = "$name = ${v.value!!}" + semicolonIfNecessary()

    override fun instanceVariableSetDeclaration(name: String, v: Value) = "this.$name = ${v.value!!}" + semicolonIfNecessary()

    override fun methodCallDeclaration(className: Type, methodName: String, vararg parameters: Value) =
        "${className.className}.$methodName(${parameters.csv()})" + semicolonIfNecessary()

    override fun methodCallDeclaration(methodName: String, vararg parameters: Value) =
        "$methodName(${parameters.csv()})" + semicolonIfNecessary()

    override fun methodDeclaration(
        vis: Visibility,
        returnType: Type,
        name: String,
        vararg parameters: Parameter,
        isStatic: Boolean,
        isFinal: Boolean,
        isOverride: Boolean
    ): Pair<String?, String> {
        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        return Pair(if(isOverride) {
            "@Override"
        } else null,
            "${vis.name.lowercase()} $static$final${returnType.className} $name(${parameters.csv()})"
        )
    }

    override fun returnDeclaration(value: Value?) =
        (if(value != null) {
            "return ${value.value!!}"
        } else "return") + semicolonIfNecessary()

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for(${variable.type.className} ${variable.value} : ${iterable.value})"

    override fun whileLoopDeclaration(condition: Condition) = "while(${condition.value})"

    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        implements: Array<Type>?,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        val e = if(extends != null) "extends ${extends.shortNameWithGenerics} " else ""
        val i = if(implements?.isNotEmpty() == true) "implements ${implements.csv()} " else ""

        return "${vis.name.lowercase()} $static${final}class $name $e$i"
    }

    override fun enumClassDeclaration(name: String, vararg values: String) = "enum $name { ${values.csv() } "

    override fun block(start: String, body: Scope, tabs: String): String {
        val bodyStr = body.get()
        val endWhitespaceLine = if(!bodyStr.endsWith("\n")) "\n" else ""

        return "$tabs${start.trim()} {\n$bodyStr$endWhitespaceLine$tabs}"
    }

    open fun importDeclaration(importPath: String, className: String) = "import ${importPath}.${className}" + semicolonIfNecessary()

    override fun new(type: Type, vararg parameters: Value) = Value(
        type, "new ${type.className}${if(type.hasGenerics) "<>" else ""}(${parameters.csv()})"
    )

    override fun callValue(methodName: String, returnType: Type, vararg parameters: Value) = Value(
        returnType, "$methodName(${parameters.csv()})"
    )

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
                Parameter(OpenCvTypes.Mat, "input"), isOverride = true
            )
            classBodyScope.newStatement()
        }

        classBodyScope.method(
            Visibility.PUBLIC, OpenCvTypes.Mat, "processFrame", processFrameScope,
            Parameter(OpenCvTypes.Mat, "input"), isOverride = true
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

        if(genInClass) {
            importScope.importType(OpenCvTypes.OpenCvPipeline)
        }

        mainScope.scope(importScope)
        mainScope.newStatement()

        if(genInClass) {
            mainScope.clazz(Visibility.PUBLIC, className, classBodyScope, extends = OpenCvTypes.OpenCvPipeline)
        } else {
            mainScope.scope(classBodyScope, trimIndent = true)
        }

        mainScope.get()
    }

    protected fun semicolonIfNecessary() = if(usesSemicolon) ";" else ""

    class BaseImportBuilder(val lang: LanguageBase) : Language.ImportBuilder {
        private val imports = mutableMapOf<String, MutableList<String>>()

        override fun import(type: Type) {
            if(lang.isImportExcluded(type)) return

            if(imports.containsKey(type.packagePath)) {
                val importsOfThis = imports[type.packagePath]!!

                if(importsOfThis.size > 2 && lang.optimizeImports) {
                    importsOfThis.clear()
                    importsOfThis.add("*")
                } else {
                    if(importsOfThis.size == 1 && importsOfThis[0] == "*") return

                    if(!importsOfThis.contains(type.className)) {
                        importsOfThis.add(type.className)
                    }
                }
            } else {
                imports[type.packagePath] = mutableListOf(type.className)
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