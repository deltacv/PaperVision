package io.github.deltacv.papervision.node.vision.overlay

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Scalar
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_drawrects",
    category = Category.OVERLAY,
    description = "des_drawrects"
)
open class DrawRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false) : DrawNode<DrawRectanglesNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val rectangles = ListAttribute(INPUT, RectAttribute, "$[att_rects]")

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")
    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()
        + rectangles.rebuildOnChange()

        + lineColor
        + lineThickness

        lineThickness.value.set(1) // initial value

        if (!isDrawOnInput) {
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
                val colorScalar = uniqueVariable(
                    "rectsColor",
                    Scalar.new(
                        color.a.v,
                        color.b.v,
                        color.c.v,
                        color.d.v,
                    )
                )

                val input = inputMat.value(current)
                val rectanglesList = rectangles.value(current)

                val thickness = lineThickness.value(current).value
                val thicknessVariable = uniqueVariable("rectsThickness", thickness.v)

                val output = uniqueVariable("${input.value.value!!}Rects", Mat.new())

                var drawMat = input.value

                group {
                    if (current.isForPreviz) {
                        public(thicknessVariable, lineThickness.label())
                    }

                    public(colorScalar, lineColor.label())

                    if (!isDrawOnInput) {
                        private(output)
                    }
                }

                current.scope {
                    if (!isDrawOnInput) {
                        drawMat = output
                        input.value("copyTo", drawMat)
                    }

                    if (rectanglesList !is GenValue.GList.RuntimeListOf<*>) {
                        for (rectangle in (rectanglesList as GenValue.GList.ListOf<*>).elements) {
                            if (rectangle is GenValue.GRect.Rect) {
                                Imgproc(
                                    "rectangle", drawMat,
                                    JvmOpenCvTypes.Rect.new(
                                        double(rectangle.x.value), double(rectangle.y.value),
                                        double(rectangle.w.value), double(rectangle.h.value)
                                    ),
                                    colorScalar,
                                    if (current.isForPreviz)
                                        thicknessVariable
                                    else thickness.v
                                )
                            } else if (rectangle is GenValue.GRect.RuntimeRect) {
                                Imgproc(
                                    "rectangle", drawMat, rectangle.value, colorScalar,
                                    if (current.isForPreviz)
                                        thicknessVariable
                                    else thickness.v
                                )
                            }
                        }
                    } else {
                        foreach(variable(JvmOpenCvTypes.Rect, "rect"), rectanglesList.value) {
                            Imgproc(
                                "rectangle", drawMat, it, colorScalar,
                                if (current.isForPreviz)
                                    thicknessVariable
                                else thickness.v
                            )
                        }
                    }

                    if (!isDrawOnInput) {
                        outputMat.streamIfEnabled(output, input.color)
                    }
                }

                session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if (attrib == outputMat) {
            return lastGenSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@PaperNode(
    name = "nod_drawrects_onimage",
    category = Category.OVERLAY,
    description = "des_drawrects_onimage"
)
class DrawRectanglesOnImageNode : DrawRectanglesNode(true)