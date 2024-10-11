package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.dsl.CodeGenContext
import io.github.deltacv.papervision.codegen.language.Language

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

    val sessions = mutableMapOf<GenNode<*>, CodeGenSession>()
    val busyNodes = mutableListOf<GenNode<*>>()

    val endingNodes = mutableListOf<GenNode<*>>()

    private val flags = mutableListOf<String>()

    enum class Stage {
        CREATION, INITIAL_GEN, PRE_END, ENDED_SUCCESS, ENDED_ERROR
    }

    var stage = Stage.CREATION

    fun gen() = language.gen(this)

    fun addFlag(flag: String) = if(!flags.contains(flag)) flags.add(flag) else false
    fun hasFlag(flag: String) = flags.contains(flag)
    fun flags() = flags.toTypedArray()

    val context = CodeGenContext(this)
    operator fun <T> invoke(block: CodeGenContext.() -> T) = block(context)

    data class Current(val codeGen: CodeGen, val scope: Scope, val isForPreviz: Boolean) {
        val language get() = codeGen.language

        operator fun <T> invoke(scopeBlock: CodeGenContext.() -> T) = codeGen.invoke(scopeBlock)
    }

}

class CodeGenOptions {

    var genAtTheEnd = false

    inline operator fun invoke(block: CodeGenOptions.() -> Unit) = block()

}

interface CodeGenSession

object NoSession : CodeGenSession