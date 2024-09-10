package io.github.deltacv.papervision.node.code

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_codesnippet",
    category = Category.CODE,
    description = "A user-written snippet of Java code that will be inlined in the final pipeline, with configurable parameters and outputs."
)
class CodeSnippetNode : DrawNode<CodeSnippetNode.Session>() {

    override fun drawNode() {

    }

    override fun genCode(current: CodeGen.Current): Session {
        TODO("Not yet implemented")
    }

    class Session : CodeGenSession {

    }

}