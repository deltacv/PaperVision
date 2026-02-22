package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

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
        generatorFor(JavaLanguage) {
            val session = Session()

            var firstValue = JvmOpenCv.toRuntimeVec2(first.genValue(current), current)
            var secondValue = JvmOpenCv.toRuntimeVec2(second.genValue(current), current)

            current {
                fun operate(first: GenValue.Double, second: GenValue.Double) = when (operation.genValue(current).value) {
                    Operation.PLUS -> first.v + second.v
                    Operation.MINUS -> first.v - second.v
                    Operation.MULTIPLY -> first.v * second.v
                    Operation.DIVIDE -> first.v / second.v
                }

                val xResultValue = operate(firstValue.xValue, secondValue.xValue)
                val yResultValue = operate(firstValue.yValue, secondValue.yValue)

                session.result = GenValue.Vec2.Runtime(
                    GenValue.Double.Runtime(xResultValue.resolved()),
                    GenValue.Double.Runtime(yResultValue.resolved())
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