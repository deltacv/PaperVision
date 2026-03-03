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

package io.github.deltacv.papervision.codegen.dsl.jvm

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.language.jvm.JavaTypes
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.enableJavaTargets
import io.github.deltacv.papervision.codegen.dsl.LanguageCtx
import io.github.deltacv.papervision.codegen.dsl.ScopeCtx

class JvmTargetsContext(context: LanguageCtx) {
    val rectTargets = context.run {
        DeclarableVariable("rectTargets", JavaTypes.HashMap(JavaTypes.String, JvmOpenCv.Rect).new())
    }
    val rotRectTargets = context.run {
        DeclarableVariable("rotRectTarget", JavaTypes.HashMap(JavaTypes.String, JvmOpenCv.RotatedRect).new())
    }

    fun ScopeCtx.addRectTarget(label: Value, rect: Value) {
        "addRectTarget"(label, rect)
    }

    fun ScopeCtx.addRotRectTarget(label: Value, rect: Value) {
        "addRotRectTarget"(label, rect)
    }

    fun ScopeCtx.clearTargets() {
        "clearTargets"()
    }
}

fun <T> CodeGen.Current.jvmTargets(enableTargetsIfNeeded: Boolean = true, block: JvmTargetsContext.() -> T): T {
    if(enableTargetsIfNeeded) {
        enableJavaTargets()
    }

    return block(JvmTargetsContext(codeGen.context))
}
