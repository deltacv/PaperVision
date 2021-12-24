package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Scope
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.dsl.CodeGenContext
import io.github.deltacv.easyvision.codegen.language.Language
import io.github.deltacv.easyvision.node.Node

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED
}

class CodeGen(var className: String, val language: Language) {

    val importScope     = Scope(0, language)
    val classStartScope = Scope(1, language, importScope)
    val classEndScope   = Scope(1, language, importScope)

    val initScope     = Scope(2, language, importScope)
    val currScopeInit = Current(this, initScope)

    val processFrameScope     = Scope(2, language, importScope)
    val currScopeProcessFrame = Current(this, processFrameScope)

    val viewportTappedScope     = Scope(2, language, importScope)
    val currScopeViewportTapped = Current(this, viewportTappedScope)

    val sessions = mutableMapOf<Node<*>, CodeGenSession>()

    fun gen(): String {
        val mainScope = Scope(0, language, importScope)
        val bodyScope = Scope(1, language, importScope)

        val start = classStartScope.get()
        if(start.isNotBlank()) {
            bodyScope.scope(classStartScope)
            bodyScope.newStatement()
        }

        val init = initScope.get()
        if(init.isNotBlank()) {
            bodyScope.method(
                Visibility.PUBLIC, language.VoidType, "init", initScope,
                Parameter(OpenCvTypes.Mat, "input"), isOverride = true
            )
            bodyScope.newStatement()
        }

        bodyScope.method(
            Visibility.PUBLIC, OpenCvTypes.Mat, "processFrame", processFrameScope,
            Parameter(OpenCvTypes.Mat, "input"), isOverride = true
        )

        val viewportTapped = viewportTappedScope.get()
        if(viewportTapped.isNotBlank()) {
            bodyScope.newStatement()

            bodyScope.method(
                Visibility.PUBLIC, language.VoidType, "onViewportTapped", viewportTappedScope,
                isOverride = true
            )
        }

        val end = classEndScope.get()
        if(end.isNotBlank()) {
            bodyScope.scope(classEndScope)
        }

        mainScope.scope(importScope)
        mainScope.newStatement()
        mainScope.clazz(Visibility.PUBLIC, className, bodyScope, extends = OpenCvTypes.OpenCvPipeline)

        return mainScope.get()
    }

    private val context = CodeGenContext(this)

    operator fun <T> invoke(block: CodeGenContext.() -> T) = block(context)

    data class Current(val codeGen: CodeGen, val scope: Scope) {
        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

interface CodeGenSession

object NoSession : CodeGenSession