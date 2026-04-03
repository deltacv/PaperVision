package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.gui.font.Font
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_integermath",
    category = NodeCategory.MATH,
    description = "des_integermath"
)
@CodecType
class IntegerMathNode : DrawNode<IntegerMathNode.Session>() {

    val first = IntAttribute(INPUT, "$[att_first]")
    val operation = EnumAttribute(INPUT, "$[att_operation]", Operation.entries, Font.find("font-awesome")) { it.icon }
    val second = IntAttribute(INPUT, "$[att_second]")

    val result = IntAttribute(OUTPUT, "$[att_result]")

    override fun onEnable() {
        +first
        +operation.rebuildOnChange()
        +second

        +result
    }

    override val generators = generatorsBuilder {
        generatorForAny {
            val session = Session()

            val firstValue = first.genValue(current)
            val secondValue = second.genValue(current)

            current {
                var firstV = firstValue.v
                var secondV = secondValue.v

                // move into class variables for previz so they can be tuned without rebuilding
                if (codeGen.isForPreviz) {
                    if (firstValue is GenValue.Int.Actual)
                        firstV = uniqueVariable("integerMathFirst", firstValue.v)
                    if (secondValue is GenValue.Int.Actual)
                        secondV = uniqueVariable("integerMathSecond", secondValue.v)

                    group {
                        if (firstV is DeclarableVariable) public(firstV, first.label())
                        if (secondV is DeclarableVariable) public(secondV, second.label())
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

                session.result = GenValue.Int.Runtime(resultValue.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when (attrib) {
        result -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.result }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("first", first)
        encoder.obj("operation", operation)
        encoder.obj("second", second)
        encoder.obj("result", result)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("first", first)
        decoder.obj("operation", operation)
        decoder.obj("second", second)
        decoder.obj("result", result)
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Int.Runtime
    }
}