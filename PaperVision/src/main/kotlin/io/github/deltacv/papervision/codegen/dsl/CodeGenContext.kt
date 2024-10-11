package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.*
import io.github.deltacv.papervision.codegen.build.*

class CodeGenContext(val codeGen: CodeGen) : LanguageContext(codeGen.language) {

    val isForPreviz get() = codeGen.isForPreviz

    fun enum(name: String, vararg values: String) {
        codeGen.classStartScope.enumClass(name, *values)
    }

    fun init(block: ScopeContext.() -> Unit) {
        codeGen.initScope(block)
    }

    fun processFrame(block: ScopeContext.() -> Unit) {
        codeGen.processFrameScope(block)
    }

    fun onViewportTapped(block: ScopeContext.() -> Unit) {
        codeGen.viewportTappedScope(block)
    }

    fun public(variable: Variable, label: String? = null) =
        codeGen.classStartScope.instanceVariable(Visibility.PUBLIC, variable, label)

    fun private(variable: Variable) =
        codeGen.classStartScope.instanceVariable(Visibility.PRIVATE, variable, null)

    fun protected(variable: Variable) =
        codeGen.classStartScope.instanceVariable(Visibility.PROTECTED, variable, null)

    private var isFirstGroup = true

    fun group(scope: Scope = codeGen.classStartScope, block: () -> Unit) {
        if(!isFirstGroup) {
            scope.newLineIfNotBlank()
        }
        isFirstGroup = false

        block()
    }

    fun uniqueVariable(name: String, value: Value) = variable(tryName(name), value)

    fun tryName(name: String) = codeGen.classStartScope.tryName(name)

    operator fun String.invoke(
        vis: Visibility, returnType: Type,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = true,
        scopeBlock: ScopeContext.() -> Unit
    ) {
        val s = Scope(2, codeGen.language)
        scopeBlock(s.context)

        codeGen.classEndScope.method(
            vis, returnType, this, s, *parameters,
            isStatic = isStatic, isFinal = isFinal, isOverride = isOverride
        )
    }

}