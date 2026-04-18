package org.deltacv.papervision.node.math

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.math.IntAttribute
import org.deltacv.papervision.attribute.misc.EnumAttribute
import org.deltacv.papervision.attribute.rebuildOnChange
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.language.cpython.CPythonTypes
import org.deltacv.papervision.codegen.build.language.jvm.JavaTypes
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import org.deltacv.papervision.codegen.language.jvm.JavaLanguage
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder

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
@CodecType
class DecimalToIntegerNode : DrawNode<DecimalToIntegerNode.Session>(){

    val input = DoubleAttribute(INPUT, "$[att_input]")
    val roundingBehavior = EnumAttribute(INPUT, "$[att_roundingbehavior]", RoundingBehavior.entries) { it.icon }
    val output = IntAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input
        + roundingBehavior.rebuildOnChange()
        + output
    }

    override val generators = polyglot {
        generatorFor(JavaLanguage) {
            val session = Session()

            val inputValue = input.genValue(current)
            val roundingBehaviorValue = roundingBehavior.genValue(current)

            current {
                var inputV = inputValue.v

                if(codeGen.isForPreviz && inputValue is GenValue.Double.Actual) {
                    inputV = uniqueVariable("decimalToIntegerInput", inputV)

                    group {
                        public(inputV, input.tunerLabel())
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

        generatorFor(CPythonLanguage) {
            val session = Session()

            val inputValue = input.genValue(current)
            val roundingBehaviorValue = roundingBehavior.genValue(current)

            current {
                val inputV = inputValue.v

                val result = when(roundingBehaviorValue.value) {
                    RoundingBehavior.ROUND -> "round".callValue(CPythonLanguage.NoType, inputV)
                    RoundingBehavior.FLOOR -> int(CPythonTypes.math.callValue("floor", CPythonLanguage.NoType, inputV))
                    RoundingBehavior.CEIL -> int(CPythonTypes.math.callValue("ceil", CPythonLanguage.NoType, inputV))
                    RoundingBehavior.TRUNCATE -> int(inputV) // int() in python truncates towards zero
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

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("roundingBehavior", roundingBehavior)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("input", input)
        decoder.obj("roundingBehavior", roundingBehavior)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Int.Runtime
    }
}



