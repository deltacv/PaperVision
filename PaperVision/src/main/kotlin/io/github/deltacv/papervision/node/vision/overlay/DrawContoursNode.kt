package io.github.deltacv.papervision.node.vision.overlay

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Scalar
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_drawcontours",
    category = Category.OVERLAY,
    description = "des_drawcontours"
)
open class DrawContoursNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false)
    : DrawNode<DrawContoursNode.Session>()  {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val contours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")

    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()
        + contours.rebuildOnChange()

        + lineColor
        + lineThickness

        lineThickness.value.set(1) // initial value

        if(!isDrawOnInput) {
            + outputMat.enablePrevizButton().rebuildOnChange()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val color = lineColor.value(current)
                val colorScalar = uniqueVariable("contoursColor",
                    Scalar.new(
                        color.a.v,
                        color.b.v,
                        color.c.v,
                        color.d.v,
                    )
                )

                val input = inputMat.value(current)
                val contoursList = contours.value(current)

                val thickness = lineThickness.value(current).value
                val thicknessVariable = uniqueVariable("contoursThickness", thickness.v)

                val output = uniqueVariable("${input.value.value!!}Contours", Mat.new())

                if(contoursList !is GenValue.GList.RuntimeListOf<*>) {
                    contours.raise("Given list is not a runtime type (TODO)") // TODO: Handle non-runtime lists
                }

                var drawMat = input.value

                group {
                    if(current.isForPreviz) {
                        public(thicknessVariable, lineThickness.label())
                    }

                    public(colorScalar, lineColor.label())

                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    if(!isDrawOnInput) {
                        drawMat = output
                        input.value("copyTo", drawMat)
                    }

                    Imgproc("drawContours", drawMat, contoursList.value, (-1).v, colorScalar,
                        if(isForPreviz)
                            thicknessVariable
                        else thickness.v
                    )

                    if(!isDrawOnInput) {
                        outputMat.streamIfEnabled(drawMat, input.color)
                    }
                }

                session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return lastGenSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@PaperNode(
    name = "nod_drawcontours_onimage",
    category = Category.OVERLAY,
    description = "des_drawcontours_onimage"
)
class DrawContoursOnImageNode : DrawContoursNode(true)