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

package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.Resolvable
import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.*
import io.github.deltacv.papervision.node.vision.ColorSpace

class ScopeContext(val scope: Scope) : LanguageContext(scope.language) {

    var appendWhiteline: Boolean
        get() = scope.appendWhiteline
        set(value) { scope.appendWhiteline = value }

    operator fun String.invoke(vararg parameters: Value) {
        scope.methodCall(this, *parameters)
    }

    operator fun Type.invoke(method: String, vararg parameters: Value) {
        scope.methodCall(this, method, *parameters)
    }

    operator fun Value.invoke(method: String, vararg parameters: Value) {
        scope.methodCall(this, method, *parameters)
    }

    private var isFirstGroup = true

    fun group(block: () -> Unit) {
        if(!isFirstGroup) {
            separate()
        }

        isFirstGroup = false

        block()
    }

    fun separate(separations: Int = 1) {
        repeat(separations) {
            scope.newLineIfNotBlank()
        }
    }

    fun streamMat(id: Int, mat: Value, matColor: ColorSpace = ColorSpace.RGB) {
        streamMat(id, mat, Resolvable.Now(matColor))
    }

    fun streamMat(id: Int, mat: Value, matColor: Resolvable<ColorSpace> = Resolvable.Now(ColorSpace.RGB)) {
        scope.streamMat(id, mat, matColor)
    }

    fun MatAttribute.streamIfEnabled(mat: Value, matColor: Resolvable<ColorSpace> = Resolvable.Now(ColorSpace.RGB)) {
        if(displayWindow != null) {
            streamMat(displayWindow!!.imageDisplay.id, mat, matColor)
        }
    }

    infix fun String.local(v: Value) =
        scope.localVariable(DeclarableVariable(this, v))

    fun local(v: DeclarableVariable) = scope.localVariable(v)

    fun instanceVariable(
        vis: Visibility, variable: DeclarableVariable, label: String? = null,
        isStatic: Boolean = false, isFinal: Boolean = false
    ) = scope.instanceVariable(vis, variable, label, isStatic, isFinal)

    infix fun DeclarableVariable.set(v: Value) =
        scope.variableSet(this, v)

    fun DeclarableVariable.arraySet(index: Value, v: Value) =
        scope.arraySet(this, index, v)

    operator fun DeclarableVariable.set(index: Value, v: Value) = arraySet(index, v)

    infix fun DeclarableVariable.instanceSet(v: Value) =
        scope.instanceVariableSet(this, v)

    fun ifCondition(condition: Condition, block: ScopeContext.() -> Unit) {
        val ifScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(ifScope.context)

        scope.ifCondition(condition, ifScope)
    }

    fun <T: Value> foreach(variable: T, list: Value, block: ScopeContext.(T) -> Unit) {
        val loopScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(loopScope.context, variable)

        scope.foreachLoop(variable, list, loopScope)
    }

    fun <T: Value> forLoop(variable: T, start: Value, max: Value, step: Value?, block: ScopeContext.(T) -> Unit) {
        val loopScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(loopScope.context, variable)

        scope.forLoop(variable, start, max, step, loopScope)
    }

    fun <T: Value> forLoop(variable: T, start: Value, max: Value, block: ScopeContext.(T) -> Unit) =
        forLoop(variable, start, max, null, block)

    fun constructor(
        vis: Visibility, clazz: Type, vararg parameters: Parameter, block: ScopeContext.() -> Unit
    ) {
        val constructorScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(constructorScope.context)

        scope.constructor(vis, clazz.className, constructorScope, *parameters)
    }

    fun deferredBlock(resolvable: Resolvable<ScopeContext.() -> Unit>) {
        val block = resolvable.resolve()

        if(block != null) {
            block(scope.context)
        } else {
            val placeholder = Resolvable.DependentPlaceholder(resolvable) {
                val newScope = Scope(scope.tabsCount, scope.language, scope.importScope)
                it(newScope.context)

                newScope.get()
            }

            scope.write(placeholder.placeholder)
        }
    }

    fun method(
        vis: Visibility, returnType: Type, name: String,
        vararg parameters: Parameter, isStatic: Boolean = false,
        isFinal: Boolean = false, isOverride: Boolean = false,
        isSynchronized: Boolean = false, block: ScopeContext.() -> Unit
    ) {
        val methodScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(methodScope.context)

        scope.method(vis, returnType, name, methodScope, *parameters, isStatic = isStatic, isFinal = isFinal, isSynchronized = isSynchronized, isOverride = isOverride)
    }

    fun returnMethod(value: Value? = null) {
        scope.returnMethod(value)
    }

    fun beforeReturning(block: ScopeContext.() -> Unit) {
        scope.beforeReturning {
            block(it.context)
        }
    }

    fun clazz(
        vis: Visibility, name: String,
        extends: Type? = null, vararg implements: Type,
        isStatic: Boolean = false, isFinal: Boolean = false,
        block: ScopeContext.() -> Unit
    ) {
        val clazzScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(clazzScope.context)

        scope.clazz(vis, name, clazzScope, extends, *implements, isStatic = isStatic, isFinal = isFinal)
    }

    fun comment(text: String) {
        scope.comment(text)
    }

}
