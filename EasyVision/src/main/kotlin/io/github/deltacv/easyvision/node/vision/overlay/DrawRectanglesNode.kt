package io.github.deltacv.easyvision.node.vision.overlay

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Scalar
import io.github.deltacv.easyvision.codegen.build.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.Colors

@RegisterNode(
    name = "nod_drawrects",
    category = Category.OVERLAY,
    description = "Draws the rectangles on a copy of the given image and outputs the result."
)
open class DrawRectanglesNode
@JvmOverloads constructor(val isDrawOnInput: Boolean = false)
    : DrawNode<DrawRectanglesNode.Session>()  {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val contours = ListAttribute(INPUT, RectAttribute, "$[att_rects]")

    val lineColor = ScalarAttribute(INPUT, Colors.RGB, "$[att_linecolor]")
    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat
        + contours

        + lineColor
        + lineThickness

        lineThickness.value.set(1) // initial value

        if(!isDrawOnInput) {
            + outputMat
        } else {
            inputMat.variableName = "$[att_drawon_image]"
        }
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val color = lineColor.value(current)
        val colorScalar = tryName("contoursColor")

        val input = inputMat.value(current)
        val contoursList = contours.value(current)
        val thickness = lineThickness.value(current).value

        val output = tryName("${input.value.value!!}Contours")

        if(contoursList !is GenValue.GLists.RuntimeListOf<*>) {
            contours.raise("") // TODO: Handle non-runtime lists
        }

        var drawMat = input.value

        // add necessary imports

        group {
            public(
                colorScalar,
                Scalar.new(
                    color.a.v,
                    color.b.v,
                    color.c.v,
                    color.d.v,
                )
            )

            if (!isDrawOnInput) {
                private(output, new(OpenCvTypes.Mat))
            }
        }

        current.scope {
            if(!isDrawOnInput) {
                drawMat = output.v
                "${input.value.value}.copyTo"(drawMat)
            }

            Imgproc("drawContours",
                drawMat, contoursList.value,
                (-1).v, colorScalar.v, thickness.v
            )
        }

        session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == outputMat) {
            return genSession!!.outputMat
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