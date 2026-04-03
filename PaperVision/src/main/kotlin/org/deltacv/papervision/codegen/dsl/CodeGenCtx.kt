/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.deltacv.papervision.codegen.dsl

import org.deltacv.papervision.codegen.*
import org.deltacv.papervision.codegen.build.*

class CodeGenCtx(val codeGen: CodeGen) : LanguageCtx(codeGen.language) {

    fun enum(name: String, vararg values: String) {
        codeGen.classStartScope.enumClass(name, *values)
    }

    fun initScope(block: ScopeCtx.() -> Unit) {
        codeGen.initScope(block = block)
    }

    fun processFrameScope(block: ScopeCtx.() -> Unit) {
        codeGen.processFrameScope(block = block)
    }

    fun onViewportTappedScope(block: ScopeCtx.() -> Unit) {
        codeGen.viewportTappedScope(block = block)
    }

    fun public(variable: DeclarableVariable, label: String? = null) =
        codeGen.classStartScope.instanceVariable(Visibility.PUBLIC, variable, label)

    fun private(variable: DeclarableVariable) =
        codeGen.classStartScope.instanceVariable(Visibility.PRIVATE, variable, null)

    fun protected(variable: DeclarableVariable) =
        codeGen.classStartScope.instanceVariable(Visibility.PROTECTED, variable, null)

    private var isFirstGroup = true
    fun group(scope: Scope = codeGen.classStartScope, block: () -> Unit) {
        if(!isFirstGroup) {
            scope.newLineIfNotBlank()
        }
        isFirstGroup = false

        block()
    }

    fun uniqueVariable(name: String, value: Value, isNullable: Boolean = false) = variable(tryName(name), value, isNullable)

    fun tryName(name: String) = codeGen.classStartScope.tryName(name)

    operator fun String.invoke(
        vis: Visibility, returnType: Type,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = true,
        scopeBlock: ScopeCtx.() -> Unit
    ) {
        val s = Scope(2, codeGen.language)
        scopeBlock(ScopeCtx(s))

        codeGen.classEndScope.method(
            vis, returnType, this, s, *parameters,
            isStatic = isStatic, isFinal = isFinal, isOverride = isOverride
        )
    }

}



