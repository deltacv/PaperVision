package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.codegen.build.Scope
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.Type

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

    infix fun String.value(v: Value) =
        scope.instanceVariable(Visibility.PUBLIC, this, v)

    infix fun String.local(v: Value) =
        scope.localVariable(this, v)

    infix fun String.set(v: Value) =
        scope.variableSet(this, v)

    infix fun String.instanceSet(v: Value) =
        scope.instanceVariableSet(this, v)

    fun foreach(variable: Value, list: Value, block: ScopeContext.(Value) -> Unit) {
        val loopScope = Scope(scope.tabsCount + 1, scope.language, scope.importScope)
        block(loopScope.context, variable)

        scope.foreachLoop(variable, list, loopScope)
    }

    fun returnMethod(value: Value? = null) = scope.returnMethod(value)

}