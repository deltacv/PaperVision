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

package io.github.deltacv.papervision.codegen.build

import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.node.vision.ColorSpace

class Scope(
    val tabsCount: Int = 1,
    val language: Language,
    val importScope: Scope? = null,
    val isForPreviz: Boolean = false
) {

    private var builder = StringBuilder()

    private val usedNames = mutableListOf<String>()

    private val beforeReturningCallbacks = mutableListOf<(Scope) -> Unit>()

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
            if(value == Value.NONE) continue

            importType(*value.imports.toTypedArray())
        }
    }

    fun instanceVariable(vis: Visibility, variable: DeclarableVariable, label: String? = null,
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
    
    fun localVariable(variable: DeclarableVariable) {
        newStatement()
        usedNames.add(variable.name)
        importValue(variable)

        builder.append("$tabs${language.localVariableDeclaration(variable)}")
    }

    fun tryName(name: String): String {
        if (name !in usedNames) return name

        var count = 1
        var newName: String

        do {
            newName = "$name$count"
            count++
        } while (newName in usedNames)

        return newName
    }

    fun variableSet(variable: DeclarableVariable, v: Value) {
        newStatement()
        importValue(v)

        builder.append("$tabs${language.variableSetDeclaration(variable, v)}")
    }

    fun arraySet(variable: DeclarableVariable, index: Value, v: Value) {
        newStatement()
        importValue(v)

        builder.append("$tabs${language.arrayVariableSetDeclaration(variable, index, v)}")
    }

    fun instanceVariableSet(variable: DeclarableVariable, v: Value) {
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

    fun streamMat(id: Int, mat: Value, matColor: Resolvable<ColorSpace> = Resolvable.Now(ColorSpace.RGB)) {
        if(isForPreviz) {
            newStatement()

            val cvtCode = Resolvable.DependentPlaceholder(matColor) {
                if(it != ColorSpace.RGB) {
                    language.cvtColorValue(it, ColorSpace.RGB)
                } else Value.NONE
            }

            val declaration = Resolvable.DependentPlaceholder(cvtCode) {
                importValue(it)
                language.streamMatCallDeclaration(language.int(id), mat, it)
            }.placeholder

            builder.append("$tabs$declaration")
        }
    }

    fun methodCall(methodName: String, vararg parameters: Value) {
        newStatement()
        importValue(*parameters)

        builder.append("$tabs${language.methodCallDeclaration(methodName, *parameters)}")
    }


    fun constructor(
        vis: Visibility, className: String,
        body: Scope, vararg parameters: Parameter
    ) {
        newLineIfNotBlank()

        for(parameter in parameters) {
            importType(parameter.type)
        }

        builder.append(language.block(language.constructorDeclaration(vis, className, *parameters), body, tabsCount))
    }

    fun method(
        vis: Visibility, returnType: Type, name: String, body: Scope,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false,
        isSynchronized: Boolean = false, isOverride: Boolean = false,
        indentOverride: Int? = null
    ) {
        newLineIfNotBlank()

        for(parameter in parameters) {
            importType(parameter.type)
        }

        val methodDeclaration = language.methodDeclaration(
            vis, returnType, name, *parameters,
            isStatic = isStatic, isFinal = isFinal, isSynchronized = isSynchronized, isOverride = isOverride
        )

        if(methodDeclaration.first?.trim()?.isNotEmpty() == true) {
            for(line in methodDeclaration.first!!.split("\n")) {
                builder.append("$tabs$line").appendLine()
            }
        }

        builder.append(language.block(methodDeclaration.second, body, indentOverride ?: tabsCount))
    }

    fun beforeReturning(block: (Scope) -> Unit) {
        beforeReturningCallbacks.add(block)
    }

    fun returnMethod(value: Value? = null) {
        for(callback in beforeReturningCallbacks) {
            callback(this)
        }

        newStatement()
        if(value != null) importValue(value)

        builder.append("$tabs${language.returnDeclaration(value)}")
    }

    fun clazz(
        vis: Visibility, name: String, body: Scope,
        extends: Type? = null, vararg implements: Type,
        isStatic: Boolean = false, isFinal: Boolean = false
    ) {
        if(extends != null) importType(extends)
        importType(*implements)

        newLineIfNotBlank()

        builder.append(language.block(
            language.classDeclaration(vis, name, body, extends, *implements, isStatic = isStatic, isFinal = isFinal),
            body, tabsCount
        ))
    }

    fun enumClass(name: String, vararg values: String) {
        newStatement()

        builder.append("$tabs${language.enumClassDeclaration(name, *values)}")
    }

    private fun block(block: String, scope: Scope) {
        newStatement()
        builder.append(language.block(block, scope, tabsCount))
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

    fun scope(scope: Scope, indentOverride: Int? = null) {
        newLineIfNotBlank()

        builder.append(
            if(indentOverride != null && indentOverride >= 0)
                scope.toString().trimIndent().prependIndent("\t".repeat(indentOverride))
            else scope.toString()
        )
    }

    fun comment(text: String) {
        newStatement()
        // handle if text is multiline, add $tabs to each line
        val lines = if(text.contains("\n")) {
            language.comment(text).split("\n").joinToString("\n$tabs") { it.trim() }
        } else {
            language.comment(text)
        }

        builder.append("$tabs$lines")
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

    fun write(str: String) {
        newStatement()
        builder.append(str)
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

data class Parameter(override val type: Type, val name: String, val isFinal: Boolean = false) : Value() {
    override val value = name
}