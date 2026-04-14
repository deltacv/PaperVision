package org.deltacv.papervision.node.vision.imageproc

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.rebuildOnChange
import org.deltacv.papervision.attribute.vision.MatAttribute
import org.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.language.cpython.CPythonOpenCv.cv2
import org.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
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
    val output = ScalarAttribute(OUTPUT, ColorSpace.GENERIC, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + output
        
        output.bindColorSpace(input)
    }

    override val generators = polyglot {
        generatorFor(JavaLanguage) {
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

                for (i in 0 until 4) {
                    session.elementGenValues[i] = GenValue.Double.Runtime(
                        outputVar.resolved().map { it.propertyValue("val", DoubleType.arrayType())[i.v, DoubleType] }
                    )
                }
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val inputValue = input.genValue(current)

                current.scope {
                    val outputVar = uniqueVariable("${inputValue.value.v}_avg", cv2.callValue("mean", CPythonLanguage.NoType, inputValue.value.v))
                    local(outputVar)

                    session.output = GenValue.Scalar.Inst(outputVar.resolved())

                    for (i in 0 until 4) {
                        session.elementGenValues[i] = GenValue.Double.Runtime(
                            outputVar.resolved().map { it[i.v, DoubleType] }
                        )
                    }
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when {
        attrib == output -> GenValue.Scalar.Inst.defer { current.sessionOf(this)?.output }
        else -> {
            val index = output.findIndex(attrib)
            if (index != null) {
                GenValue.Double.Runtime.defer { current.sessionOf(this)?.elementGenValues?.get(index) }
            } else noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Scalar.Inst
        val elementGenValues = mutableMapOf<Int, GenValue.Double.Runtime>()
    }

}