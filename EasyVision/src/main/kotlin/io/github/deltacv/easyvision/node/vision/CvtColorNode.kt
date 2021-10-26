package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.*
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.parse.*
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode

@RegisterNode(
    name = "nod_cvtcolor",
    category = Category.COLOR_OP,
    description = "Converts a Mat from its current color space to the specified color space. If the mat is already in the specified color space, no conversion is made."
)
class CvtColorNode : DrawNode<CvtColorNode.Session>() {

    val input  = MatAttribute(INPUT, "Input")
    val output = MatAttribute(OUTPUT, "Output")

    val convertTo = EnumAttribute(INPUT, Colors.values(), "Convert To")

    override fun onEnable() {
        + input
        + convertTo

        + output
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputMat = input.value(current)
        inputMat.requireNonBinary(input)

        var targetColor = convertTo.value(current).value
        var matColor = inputMat.color

        if(matColor != targetColor) {
            if(matColor == Colors.RGBA && targetColor != Colors.RGB) {
                matColor = Colors.RGB
            } else if(matColor != Colors.RGB && targetColor == Colors.RGBA) {
                targetColor = Colors.RGB
            }
        }

        import("org.opencv.imgproc.Imgproc")

        if(matColor != targetColor) {
            val matName = tryName("${targetColor.name.lowercase()}Mat")

            group {
                // create mat instance variable
                private(matName, new("Mat"))
            }

            current.scope { // add a cvtColor step in processFrame
                "Imgproc.cvtColor"(inputMat.value, matName.v, cvtColorValue(matColor, targetColor))
            }

            session.outputMatValue = GenValue.Mat(matName.v, targetColor) // store data in the current session
        } else {
            // we don't need to do any processing if the mat is
            // already of the color the user specified to convert to
            session.outputMatValue = inputMat
        }

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return genSession!!.outputMatValue
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}