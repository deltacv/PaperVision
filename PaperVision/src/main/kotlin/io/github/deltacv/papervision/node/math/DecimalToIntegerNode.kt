package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.language.jvm.JavaTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode

enum class RoundingBehavior(val icon: String) {
    ROUND("mis_round"),
    FLOOR("mis_floor"),
    CEIL("mis_ceil"),
    TRUNCATE("mis_truncate")
}

@PaperNode(
    name = "nod_decimalto_integer",
    category = NodeCategory.MATH,
    description = "des_decimalto_integer"
)
class DecimalToIntegerNode : DrawNode<DecimalToIntegerNode.Session>(){

    val input = DoubleAttribute(INPUT, "$[att_input]")
    val roundingBehavior = EnumAttribute(INPUT, "$[att_roundingbehavior]", RoundingBehavior.entries) { it.icon }
    val output = IntAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input
        + roundingBehavior.rebuildOnChange()
        + output
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val inputValue = input.genValue(current)
            val roundingBehaviorValue = roundingBehavior.genValue(current)

            current {
                var inputV = inputValue.v

                if(codeGen.isForPreviz && inputValue is GenValue.Double.Actual) {
                    inputV = uniqueVariable("decimalToIntegerInput", inputV)

                    group {
                        public(inputV, input.label())
                    }
                }

                val result = when(roundingBehaviorValue.value) {
                    RoundingBehavior.ROUND -> JavaTypes.Math.callValue("round", LongType, inputV)
                    RoundingBehavior.FLOOR -> JavaTypes.Math.callValue("floor", DoubleType, inputV).castTo(IntType)
                    RoundingBehavior.CEIL -> JavaTypes.Math.callValue("ceil", DoubleType, inputV).castTo(IntType)
                    RoundingBehavior.TRUNCATE -> int(inputV) // cast to int in java truncates towards zero
                }

                session.output = GenValue.Int.Runtime(result.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.output }
        else -> noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Int.Runtime
    }
}