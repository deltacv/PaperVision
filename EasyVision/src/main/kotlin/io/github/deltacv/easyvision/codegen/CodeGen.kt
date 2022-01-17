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

class CodeGen(
    var className: String,
    val language: Language,
    val isForPreviz: Boolean = false
) {

    val importScope     = Scope(0, language)
    val classStartScope = Scope(1, language, importScope, isForPreviz)
    val classEndScope   = Scope(1, language, importScope, isForPreviz)

    val initScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeInit = Current(this, initScope, isForPreviz)

    val processFrameScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeProcessFrame = Current(this, processFrameScope, isForPreviz)

    val viewportTappedScope     = Scope(2, language, importScope, isForPreviz)
    val currScopeViewportTapped = Current(this, viewportTappedScope, isForPreviz)

    val sessions = mutableMapOf<Node<*>, CodeGenSession>()
    val busyNodes = mutableListOf<Node<*>>()

    fun gen() = language.gen(this)

    private val context = CodeGenContext(this)

    operator fun <T> invoke(block: CodeGenContext.() -> T) = block(context)

    data class Current internal constructor(val codeGen: CodeGen, val scope: Scope, val isForPreviz: Boolean) {
        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

interface CodeGenSession

object NoSession : CodeGenSession