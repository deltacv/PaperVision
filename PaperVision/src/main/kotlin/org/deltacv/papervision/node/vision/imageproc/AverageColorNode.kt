package org.deltacv.papervision.node.vision.imageproc

import org.deltacv.papervision.attribute.vision.MatAttribute
import org.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.codegen.language.BaseLanguage
import org.deltacv.papervision.codegen.language.jvm.JavaLanguage
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_averagecolor",
    category = NodeCategory.IMAGE_PROC,
    description = "des_averagecolor"
)
class AverageColorNode : DrawNode<AverageColorNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val output = ScalarAttribute(OUTPUT, ColorSpace.RGB, "$[att_output]")

    override fun onEnable() {
        + input
        + output
    }

    override val generators = generatorsBuilder {
        generatorFor<BaseLanguage> {
            val session = Session()

            current {
                val inputValue = input.genValue(current)

                val outputVar = uniqueVariable("${inputValue.value.v}Avg", JvmOpenCv.Scalar.new())

                group {
                    private(outputVar)
                }

                current.scope {
                    outputVar instanceSet inputValue.value.v.callValue("mean", JvmOpenCv.Scalar)
                }

                session.output = GenValue.Scalar.Inst(outputVar.resolved())
            }

            session
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Scalar
    }

}