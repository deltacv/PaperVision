package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.node.vision.Colors

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

    fun streamMat(id: Int, mat: Value, matColor: Colors = Colors.RGB) {
        scope.streamMat(id, mat, matColor)
    }

    fun MatAttribute.streamIfEnabled(mat: Value, matColor: Colors = Colors.RGB) {
        if(displayWindow != null) {
            streamMat(displayWindow!!.displayId, mat, matColor)
        }
    }

    infix fun String.local(v: Value) =
        scope.localVariable(Variable(this, v))

    fun local(v: Variable) = scope.localVariable(v)

    infix fun Variable.set(v: Value) =
        scope.variableSet(this, v)

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

    fun returnMethod(value: Value? = null) = scope.returnMethod(value)

}