package io.github.deltacv.papervision.attribute.decomp.vision

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.Node

class MatAttributeDecomposer(
    decomposerNode: Node<*>
) : AttributeDecomposer<MatAttributeDecomposer.Session>(decomposerNode) {

    val rows = IntAttribute(OUTPUT, "$[att_rows]")
    val cols = IntAttribute(OUTPUT, "$[att_columns]")

    override fun onEnable() {
        + rows
        + cols
    }

    override val generators = generatorsBuilder<GenValue, Session> {
        generatorFor(JavaLanguage) {
            val session = Session()



            current.scope {
                // session.rows = GenValue.Int(genInput.value.v.callValue("rows", IntType).resolved())
            }

            session
        }
    }

    override fun getGenValueOf(
        current: CodeGen.Current,
        attrib: Attribute
    ): GenValue {
        TODO("")
    }

    class Session : CodeGenSession {
        lateinit var rows: GenValue.Int
        lateinit var columns: GenValue.Int
    }
}