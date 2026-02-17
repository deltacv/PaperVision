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

            genInput as GenValue.Mat

            current.scope {
                session.rows = GenValue.Int.Runtime(genInput.value.v.callValue("rows", IntType).resolved())
                session.columns = GenValue.Int.Runtime(genInput.value.v.callValue("cols", IntType).resolved())
            }

            session
        }
    }

    override fun getGenValueOf(
        current: CodeGen.Current,
        attrib: Attribute
    ) = when(attrib) {
        rows -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.rows }
        cols -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.columns }
        else -> noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var rows: GenValue.Int.Runtime
        lateinit var columns: GenValue.Int.Runtime
    }
}