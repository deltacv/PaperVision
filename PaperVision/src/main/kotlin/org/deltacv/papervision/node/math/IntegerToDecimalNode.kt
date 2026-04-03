package org.deltacv.papervision.node.math

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.math.IntAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.codegen.language.jvm.JavaLanguage
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_integerto_decimal",
    category = NodeCategory.MATH,
    description = "des_integerto_decimal"
)
@CodecType
class IntegerToDecimalNode : DrawNode<IntegerToDecimalNode.Session>(){

    val input = IntAttribute(INPUT, "$[att_input]")
    val output = DoubleAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input
        + output
    }

    override val generators = generatorsBuilder {
        generatorForAny {
            val session = Session()

            val inputValue = input.genValue(current)

            current {
                var inputV = inputValue.v

                if(codeGen.isForPreviz && inputValue is GenValue.Int.Actual) {
                    inputV = uniqueVariable("decimalToIntegerInput", inputV)

                    group {
                        public(inputV, input.label())
                    }
                }

                session.output = GenValue.Double.Runtime(double(inputV).resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.output }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Double.Runtime
    }
}



