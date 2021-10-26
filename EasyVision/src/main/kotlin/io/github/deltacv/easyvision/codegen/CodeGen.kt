package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.parse.Parameter
import io.github.deltacv.easyvision.codegen.parse.Scope
import io.github.deltacv.easyvision.codegen.dsl.CodeGenContext
import io.github.deltacv.easyvision.node.Node

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED
}

class CodeGen(var className: String) {

    val importScope     = Scope(0)
    val classStartScope = Scope(1)
    val classEndScope   = Scope(1)

    val initScope     = Scope(2)
    val currScopeInit = Current(this, initScope)

    val processFrameScope     = Scope(2)
    val currScopeProcessFrame = Current(this, processFrameScope)

    val viewportTappedScope     = Scope(2)
    val currScopeViewportTapped = Current(this, viewportTappedScope)

    val sessions = mutableMapOf<Node<*>, CodeGenSession>()

    init {
        importScope.run {
            import("org.openftc.easyopencv.OpenCvPipeline")
            import("org.opencv.core.Mat")
        }
    }

    fun gen(): String {
        val mainScope = Scope(0)
        val bodyScope = Scope(1)

        val start = classStartScope.get()
        if(start.isNotBlank()) {
            bodyScope.scope(classStartScope)
            bodyScope.newStatement()
        }

        val init = initScope.get()
        if(init.isNotBlank()) {
            bodyScope.method(
                Visibility.PUBLIC, "void", "init", initScope,
                Parameter("Mat", "input"), isOverride = true
            )
            bodyScope.newStatement()
        }

        val process = processFrameScope.get()
        bodyScope.method(
            Visibility.PUBLIC, "Mat", "processFrame", processFrameScope,
            Parameter("Mat", "input"), isOverride = true
        )

        val viewportTapped = viewportTappedScope.get()
        if(viewportTapped.isNotBlank()) {
            bodyScope.newStatement()

            bodyScope.method(
                Visibility.PUBLIC, "Mat", "onViewportTapped", viewportTappedScope,
                isOverride = true
            )
        }

        val end = classEndScope.get()
        if(end.isNotBlank()) {
            bodyScope.scope(classEndScope)
        }

        mainScope.scope(importScope)
        mainScope.newStatement()
        mainScope.clazz(Visibility.PUBLIC, className, bodyScope, extends = arrayOf("OpenCvPipeline"))

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