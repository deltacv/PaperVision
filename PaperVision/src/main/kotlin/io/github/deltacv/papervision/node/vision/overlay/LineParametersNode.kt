package io.github.deltacv.papervision.node.vision.overlay

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_lineparameters",
    category = Category.OVERLAY,
    description = "des_lineparameters"
)
class LineParametersNode : DrawNode<LineParametersNode.Session>() {

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")
    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val output = LineParametersAttribute(OUTPUT, "$[att_params]")

    override fun onEnable() {
        + lineColor
        + lineThickness

        lineThickness.value.set(3)

        + output.rebuildOnChange()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val lineColorValue = lineColor.value(current)

                val lineColorVar = uniqueVariable("lineColor", JvmOpenCvTypes.Scalar.new(
                    lineColorValue.a.v, lineColorValue.b.v, lineColorValue.c.v, lineColorValue.d.v
                ))

                val lineThicknessVar = uniqueVariable("lineThickness", lineThickness.value(current).value.v)

                group {
                    public(lineColorVar, lineColor.label())
                    public(lineThicknessVar, lineThickness.label())
                }

                session.lineParameters = GenValue.LineParameters.RuntimeLine(lineColorVar, lineThicknessVar)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session.lineParameters = GenValue.LineParameters.Line(
                lineColor.value(current),
                lineThickness.value(current)
            )

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when (attrib) {
            output -> current.nonNullSessionOf(this).lineParameters
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var lineParameters: GenValue.LineParameters
    }

}