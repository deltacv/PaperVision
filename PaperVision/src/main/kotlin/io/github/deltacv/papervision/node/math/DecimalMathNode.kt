package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.gui.util.Font
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_decimalmath",
    category = NodeCategory.MATH,
    description = "des_decimalmath"
)
class DecimalMathNode : DrawNode<DecimalMathNode.Session>() {

    val first = DoubleAttribute(INPUT, "$[att_first]")
    val operation = EnumAttribute(INPUT, "$[att_operation]", Operation.entries, Font.find("font-awesome")) { it.icon }
    val second = DoubleAttribute(INPUT, "$[att_second]")

    val result = DoubleAttribute(OUTPUT, "$[att_result]")

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
                var firstV = firstValue.v
                var secondV = secondValue.v

                // move into class variables for previz so they can be tuned without rebuilding
                if(codeGen.isForPreviz) {
                    if(firstValue is GenValue.Double.Actual)
                        firstV = uniqueVariable("integerMathFirst", firstValue.v)
                    if(secondValue is GenValue.Double.Actual)
                        secondV = uniqueVariable("integerMathSecond", secondValue.v)

                    group {
                        if(firstV is DeclarableVariable) public(firstV, first.label())
                        if(secondV is DeclarableVariable) public(secondV, second.label())
                    }
                }

                val resultValue = when(operation.genValue(current).value) {
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

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            result -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.result }
            else -> super.getGenValueOf(current, attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Double.Runtime
    }
}