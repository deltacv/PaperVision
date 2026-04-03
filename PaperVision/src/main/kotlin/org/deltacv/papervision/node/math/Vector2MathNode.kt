package org.deltacv.papervision.node.math

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.misc.EnumAttribute
import org.deltacv.papervision.attribute.rebuildOnChange
import org.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.gui.font.Font
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_vector2math",
    category = NodeCategory.MATH,
    description = "des_vector2math"
)
@CodecType
class Vector2MathNode : DrawNode<Vector2MathNode.Session>() {

    val first = Vector2Attribute(INPUT, "$[att_first]")
    val operation = EnumAttribute(INPUT, "$[att_operation]", Operation.entries, Font.find("font-awesome")) { it.icon }
    val second = Vector2Attribute(INPUT, "$[att_second]")

    val result = Vector2Attribute(OUTPUT, "$[att_result]")

    override fun onEnable() {
        +first
        +operation.rebuildOnChange()
        +second

        +result
    }

    override val generators = generatorsBuilder {
        generatorForAny {
            val session = Session()

            var firstValue = first.genValue(current).toRuntime(current)
            var secondValue = second.genValue(current).toRuntime(current)

            current {
                fun operate(first: GenValue.Int, second: GenValue.Int) = when (operation.genValue(current).value) {
                    Operation.PLUS -> first.v + second.v
                    Operation.MINUS -> first.v - second.v
                    Operation.MULTIPLY -> first.v * second.v
                    Operation.DIVIDE -> first.v / second.v
                }

                val xResultValue = operate(firstValue.xValue, secondValue.xValue)
                val yResultValue = operate(firstValue.yValue, secondValue.yValue)

                session.result = GenValue.Vec2.Runtime(
                    GenValue.Int.Runtime(xResultValue.resolved()),
                    GenValue.Int.Runtime(yResultValue.resolved())
                )
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when (attrib) {
        result -> GenValue.Vec2.Runtime.defer { current.sessionOf(this)?.result }
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
        lateinit var result: GenValue.Vec2.Runtime
    }
}



