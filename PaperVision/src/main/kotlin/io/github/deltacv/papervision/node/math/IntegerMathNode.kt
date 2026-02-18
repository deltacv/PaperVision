package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_integermath",
    category = NodeCategory.MATH,
    description = "des_integermath"
)
class IntegerMathNode : DrawNode<IntegerMathNode.Session>() {

    val first = IntAttribute(INPUT, "$[att_first]")
    val operation = EnumAttribute(INPUT, Operation.entries, "$[att_operation]")
    val second = IntAttribute(INPUT, "$[att_second]")

    val result = IntAttribute(OUTPUT, "$[att_result]")

    override fun onEnable() {
        + first
        + operation.rebuildOnChange()
        + second

        + result
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val firstValue = first.genValue(current)
            val secondValue = second.genValue(current)

            current {

                val resultValue = when(operation.genValue(current).value) {
                    Operation.PLUS -> firstValue.v + secondValue.v
                    Operation.MINUS -> firstValue.v - secondValue.v
                    Operation.MULTIPLY -> firstValue.v * secondValue.v
                    Operation.DIVIDE -> {
                        firstValue.v / secondValue.v
                    }
                }

                session.result = GenValue.Int.Runtime(resultValue.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            result -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.result }
            else -> super.getGenValueOf(current, attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Int.Runtime
    }
}