package io.github.deltacv.papervision.codegen.dsl

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.vision.enableTargets

class TargetsContext(context: LanguageContext) {
    val TargetType = Type("Target", "")

    val targets = context.run {
        Variable("targets", JavaTypes.ArrayList(TargetType).new())
    }

    fun ScopeContext.addTarget(label: Value, rect: Value) {
        "addTarget"(label, rect)
    }

    fun ScopeContext.addTargets(label: Value, rects: Value) {
        "addTargets"(label, rects)
    }

    fun ScopeContext.clearTargets() {
        "clearTargets"()
    }
}

fun <T> CodeGen.Current.targets(enableTargetsIfNeeded: Boolean = true, block: TargetsContext.() -> T): T {
    if(enableTargetsIfNeeded) {
        enableTargets()
    }

    return block(TargetsContext(codeGen.context))
}
