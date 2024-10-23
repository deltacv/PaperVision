/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
