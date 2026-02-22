package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
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
    name = "nod_integerto_decimal",
    category = NodeCategory.MATH,
    description = "des_integerto_decimal"
)
class IntegerToDecimalNode : DrawNode<IntegerToDecimalNode.Session>(){

    val input = IntAttribute(INPUT, "$[att_input]")
    val output = DoubleAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input
        + output
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
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

    class Session : CodeGenSession {
        lateinit var output: GenValue.Double.Runtime
    }
}