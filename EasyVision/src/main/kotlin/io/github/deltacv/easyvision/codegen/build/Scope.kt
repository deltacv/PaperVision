package io.github.deltacv.easyvision.codegen.build

import io.github.deltacv.easyvision.codegen.dsl.ScopeContext
import io.github.deltacv.easyvision.codegen.*
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.node.vision.Colors

class Scope(
    val tabsCount: Int = 1,
    val language: Language,
    val importScope: Scope? = null,
    val isForPreviz: Boolean = false
) {

    private var builder = StringBuilder()

    private val usedNames = mutableListOf<String>()

    private val tabs by lazy {
        val builder = StringBuilder()

        repeat(tabsCount) {
            builder.append("\t")
        }

        builder.toString()
    }

    private val importBuilder = language.newImportBuilder()

    fun importType(vararg types: Type) {
        if(importScope != null) {
            importScope.importType(*types)
        } else {
            for (type in types) {
                if (type.shouldImport) {
                    importBuilder.import(type)
                }
            }
        }
    }

    private fun importValue(vararg values: Value) {
        for(value in values) {
            importType(*value.imports.toTypedArray())
        }
    }

    fun instanceVariable(vis: Visibility, variable: Variable, label: String? = null,
                         isStatic: Boolean = false, isFinal: Boolean = false) {
        newStatement()
        usedNames.add(variable.name)
        importValue(variable)

        val pair = language.instanceVariableDeclaration(
            vis, variable,
            if(isForPreviz) label else null, // labels are ignored in non-previsualization mode
            isStatic, isFinal
        )

        pair.first?.let {
            builder.appendLine("$tabs$it")
        }
        builder.append("$tabs${pair.second}")
    }
    
    fun localVariable(variable: Variable) {
        newStatement()
        usedNames.add(variable.name)
        importValue(variable)

        builder.append("$tabs${language.localVariableDeclaration(variable)}")
    }

    fun tryName(name: String): String {
        if(!usedNames.contains(name)) {
            return name
        } else {
            var count = 1

            while(true) {
                val newName = "$name$count"

                if(!usedNames.contains(newName)) {
                    return newName
                }

                count++
            }
        }
    }

    fun variableSet(variable: Variable, v: Value) {
        newStatement()
        importValue(v)

        builder.append("$tabs${language.variableSetDeclaration(variable, v)}")
    }


    fun arraySet(variable: Variable, index: Value, v: Value) {
        newStatement()
        importValue(v)

        builder.append("$tabs${language.arrayVariableSetDeclaration(variable, index, v)}")
    }

    fun instanceVariableSet(variable: Variable, v: Value) {
        newStatement()
        importValue(v)

        builder.append("$tabs${language.instanceVariableSetDeclaration(variable, v)}")
    }

    fun methodCall(className: Type, methodName: String, vararg parameters: Value) {
        newStatement()
        importType(className)
        importValue(*parameters)
        
        builder.append("$tabs${language.methodCallDeclaration(className, methodName, *parameters)}")
    }

    fun methodCall(callee: Value, methodName: String, vararg parameters: Value) {
        newStatement()
        importValue(callee, *parameters)

        builder.append("$tabs${language.methodCallDeclaration(callee, methodName, *parameters)}")
    }

    fun streamMat(id: Int, mat: Value, matColor: Colors = Colors.RGB) {
        if(isForPreviz) {
            newStatement()

            val cvtCode = if(matColor != Colors.RGB) {
                language.cvtColorValue(matColor, Colors.RGB)
            } else null

            if (cvtCode != null) importValue(cvtCode)

            builder.append("$tabs${language.streamMatCallDeclaration(id.v, mat, cvtCode)}")
        }
    }

    fun methodCall(methodName: String, vararg parameters: Value) {
        newStatement()
        importValue(*parameters)

        builder.append("$tabs${language.methodCallDeclaration(methodName, *parameters)}")
    }

    fun method(
        vis: Visibility, returnType: Type, name: String, body: Scope,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = false
    ) {
        newLineIfNotBlank()

        for(parameter in parameters) {
            importType(parameter.type)
        }

        val methodDeclaration = language.methodDeclaration(
            vis, returnType, name, *parameters,
            isStatic = isStatic, isFinal = isFinal, isOverride = isOverride
        )

        if(methodDeclaration.first?.trim()?.isNotEmpty() == true) {
            for(line in methodDeclaration.first!!.split("\n")) {
                builder.append("$tabs$line").appendLine()
            }
        }

        builder.append(language.block(methodDeclaration.second, body, tabs))
    }

    fun returnMethod(value: Value? = null) {
        newStatement()
        if(value != null) importValue(value)

        builder.append("$tabs${language.returnDeclaration(value)}")
    }

    fun clazz(vis: Visibility, name: String, body: Scope,
              extends: Type? = null, implements: Array<Type>? = null,
              isStatic: Boolean = false, isFinal: Boolean = false) {

        newStatement()

        if(extends != null) importType(extends)
        if(implements != null) importType(*implements)

        builder.append(language.block(
            language.classDeclaration(vis, name, body, extends, implements, isStatic, isFinal),
            body, tabs
        ))
    }

    fun enumClass(name: String, vararg values: String) {
        newStatement()

        builder.append("$tabs${language.enumClassDeclaration(name, *values)}")
    }

    private fun block(block: String, scope: Scope) {
        newStatement()

        builder.append(language.block(block, scope, tabs))
    }

    fun ifCondition(condition: Condition, scope: Scope) = block(language.ifStatementDeclaration(condition), scope)

    fun whileLoop(condition: Condition, scope: Scope) = block(language.whileLoopDeclaration(condition), scope)

    fun foreachLoop(variable: Value, iterable: Value, scope: Scope) {
        importValue(variable, iterable)

        block(
            language.foreachLoopDeclaration(variable, iterable),
            scope
        )
    }

    fun forLoop(variable: Value, start: Value, max: Value, step: Value?, scope: Scope) {
        importValue(variable)

        block(language.forLoopDeclaration(variable, start, max, step), scope)
    }

    fun scope(scope: Scope, trimIndent: Boolean = false) {
        newLineIfNotBlank()

        builder.append(
            if(trimIndent)
                scope.toString().trimIndent()
            else scope.toString()
        )
    }

    fun newStatement() {
        if(builder.isNotEmpty()) {
            builder.appendLine()
        }
    }

    fun newLineIfNotBlank() {
        val str = get()

        if(!str.endsWith("\n\n") && str.endsWith("\n")) {
            builder.appendLine()
        } else if(!str.endsWith("\n\n")) {
            builder.append("\n")
        }
    }

    fun clear() = builder.clear()

    fun get() = importBuilder.build() + builder.toString()

    override fun toString() = get()

    internal val context = ScopeContext(this)

    var appendWhiteline = true

    operator fun invoke(block: ScopeContext.() -> Unit) {
        block(context)

        if(appendWhiteline) {
            newStatement()
        }
        appendWhiteline = true
    }

}

data class Parameter(val type: Type, val name: String, val isFinal: Boolean = false)