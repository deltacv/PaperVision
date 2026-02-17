package io.github.deltacv.papervision.attribute.decomp.vision

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Node

class MatAttributeDecomposer(
    decomposerNode: Node<*>
) : AttributeDecomposer<MatAttribute, MatAttributeDecomposer.Session>(decomposerNode) {

    val width = IntAttribute(OUTPUT, "$[att_width]")
    val height = IntAttribute(OUTPUT, "$[att_height]")

    override fun onEnable() {
        + width
        + height
    }

    override val generators = generatorsBuilder<MatAttribute, Session> {
        generatorFor(JavaLanguage) {
            val session = Session()

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
        lateinit var width: GenValue.Int
        lateinit var height: GenValue.Int
    }
}