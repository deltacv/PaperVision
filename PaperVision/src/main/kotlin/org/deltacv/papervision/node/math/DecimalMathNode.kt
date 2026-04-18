package org.deltacv.papervision.node.math

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.misc.EnumAttribute
import org.deltacv.papervision.attribute.rebuildOnChange
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.DeclarableVariable
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_decimalmath",
    category = NodeCategory.MATH,
    description = "des_decimalmath"
)
@CodecType
class DecimalMathNode : DrawNode<DecimalMathNode.Session>() {

    val first = DoubleAttribute(INPUT, "$[att_first]")
    val operation = EnumAttribute(INPUT, "$[att_operation]", Operation.entries, Font.find("font-awesome")) { it.icon }
    val second = DoubleAttribute(INPUT, "$[att_second]")

    val result = DoubleAttribute(OUTPUT, "$[att_result]")

    override fun onEnable() {
        +first
        +operation.rebuildOnChange()
        +second

        +result
    }

    override val generators = polyglot {
        generatorForAny {
            val session = Session()

            val firstValue = first.genValue(current)
            val secondValue = second.genValue(current)

            current {
                var firstV = firstValue.v
                var secondV = secondValue.v

                // move into class variables for previz so they can be tuned without rebuilding
                if (codeGen.isForPreviz) {
                    if (firstValue is GenValue.Double.Actual)
                        firstV = uniqueVariable("integerMathFirst", firstValue.v)
                    if (secondValue is GenValue.Double.Actual)
                        secondV = uniqueVariable("integerMathSecond", secondValue.v)

                    group {
                        if (firstV is DeclarableVariable) public(firstV, first.tunerLabel())
                        if (secondV is DeclarableVariable) public(secondV, second.tunerLabel())
                    }
                }

                val resultValue = when (operation.genValue(current).value) {
                    Operation.PLUS -> firstV + secondV
                    Operation.MINUS -> firstV - secondV
                    Operation.MULTIPLY -> firstV * secondV
                    Operation.DIVIDE -> {
                        firstV / secondV
                    }
                }

                session.result = GenValue.Double.Runtime(resultValue.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when (attrib) {
        result -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.result }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder) // encode node first

        encoder.obj("first", first)
        encoder.obj("operation", operation)
        encoder.obj("second", second)
        encoder.obj("result", result)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder) // decode node first

        decoder.obj("first", first)
        decoder.obj("operation", operation)
        decoder.obj("second", second)
        decoder.obj("result", result)
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Double.Runtime
    }
}



