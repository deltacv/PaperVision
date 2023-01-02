package io.github.deltacv.easyvision.node.vision.overlay

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Mat
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Scalar
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.ColorSpace

@RegisterNode(
    name = "nod_drawrects",
    category = Category.OVERLAY,
    description = "Draws the rectangles on a copy of the given image and outputs the result."
)
open class DrawRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false)
    : DrawNode<DrawRectanglesNode.Session>()  {

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

        if(!isDrawOnInput) {
            + outputMat.enablePrevizButton()
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val color = lineColor.value(current)
        val colorScalar = uniqueVariable("rectsColor",
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

        if(rectanglesList !is GenValue.GLists.RuntimeListOf<*>) {
            rectangles.raise("") // TODO: Handle non-runtime lists
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

            foreach(variable(OpenCvTypes.Rect, "rect"), rectanglesList.value) {
                Imgproc("rectangle", drawMat, it, colorScalar,
                    if(current.isForPreviz)
                        thicknessVariable
                    else thickness.v
                )
            }

            if(!isDrawOnInput) {
                outputMat.streamIfEnabled(output, input.color)
            }
        }

        session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == outputMat) {
            return lastGenSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@RegisterNode(
    name = "nod_drawrects_onimage",
    category = Category.OVERLAY,
    description = "Draws the rectangles in the passed image."
)
class DrawRectanglesOnImageNode : DrawRectanglesNode(true)