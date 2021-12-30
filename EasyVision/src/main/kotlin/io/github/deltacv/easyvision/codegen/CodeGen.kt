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

    fun gen() = language.gen(this)

    private val context = CodeGenContext(this)

    operator fun <T> invoke(block: CodeGenContext.() -> T) = block(context)

    data class Current(val codeGen: CodeGen, val scope: Scope) {
        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

interface CodeGenSession

object NoSession : CodeGenSession