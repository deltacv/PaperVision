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

package io.github.deltacv.papervision.node.code

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

/* @PaperNode(
    name = "nod_codesnippet",
    category = Category.CODE,
    description = "A user-written snippet of Java code that will be inlined in the final pipeline, with configurable parameters and outputs."
) */
class CodeSnippetNode : DrawNode<CodeSnippetNode.Session>() {

    override fun drawNode() {

    }

    override fun genCode(current: CodeGen.Current): Session {
        TODO("Not yet implemented")
    }

    class Session : CodeGenSession {

    }

}
