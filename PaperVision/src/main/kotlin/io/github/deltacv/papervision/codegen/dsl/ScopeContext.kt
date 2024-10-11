package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.attribute.vision.MatAttribute
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

    fun separate() {
        scope.newLineIfNotBlank()
    }

    fun streamMat(id: Int, mat: Value, matColor: ColorSpace = ColorSpace.RGB) {
        scope.streamMat(id, mat, matColor)
    }

    fun MatAttribute.streamIfEnabled(mat: Value, matColor: ColorSpace = ColorSpace.RGB) {
        if(displayWindow != null) {
            streamMat(displayWindow!!.imageDisplay.id, mat, matColor)
        }
    }

    infix fun String.local(v: Value) =
        scope.localVariable(Variable(this, v))

    fun local(v: Variable) = scope.localVariable(v)

    fun instanceVariable(
        vis: Visibility, variable: Variable, label: String? = null,
        isStatic: Boolean = false, isFinal: Boolean = false
    ) = scope.instanceVariable(vis, variable, label, isStatic, isFinal)

    infix fun Variable.set(v: Value) =
        scope.variableSet(this, v)

    fun Variable.arraySet(index: Value, v: Value) =
        scope.arraySet(this, index, v)

    operator fun Variable.set(index: Value, v: Value) = arraySet(index, v)

    infix fun Variable.instanceSet(v: Value) =
        scope.instanceVariableSet(this, v)

    fun ifCondition(condition: Condition, block: ScopeContext.() -> Unit) {
        val ifScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(ifScope.context)

        scope.ifCondition(condition, ifScope)
    }

    fun foreach(variable: Value, list: Value, block: ScopeContext.(Value) -> Unit) {
        val loopScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(loopScope.context, variable)

        scope.foreachLoop(variable, list, loopScope)
    }

    fun forLoop(variable: Value, start: Value, max: Value, step: Value?, block: ScopeContext.(Value) -> Unit) {
        val loopScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(loopScope.context, variable)

        scope.forLoop(variable, start, max, step, loopScope)
    }

    fun forLoop(variable: Value, start: Value, max: Value, block: ScopeContext.(Value) -> Unit) =
        forLoop(variable, start, max, null, block)

    fun constructor(
        vis: Visibility, clazz: Type, vararg parameters: Parameter, block: ScopeContext.() -> Unit
    ) {
        val constructorScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(constructorScope.context)

        scope.constructor(vis, clazz.className, constructorScope, *parameters)
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

    fun returnMethod(value: Value? = null) = scope.returnMethod(value)

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

}