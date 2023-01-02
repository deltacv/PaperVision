package io.github.deltacv.easyvision.node.vision.imageproc

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Mat
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.ColorSpace

@RegisterNode(
    name = "nod_cvtcolor",
    category = Category.IMAGE_PROC,
    description = "Converts a Mat from its current color space to the specified color space. If the mat is already in the specified color space, no conversion is made."
)
class CvtColorNode : DrawNode<CvtColorNode.Session>() {

    val input  = MatAttribute(INPUT, "$[att_input]")
    val output = MatAttribute(OUTPUT, "$[att_output]").enablePrevizButton()

    val convertTo = EnumAttribute(INPUT, ColorSpace.values(), "$[att_convertto]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + convertTo.rebuildOnChange()

        + output.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputMat = input.value(current)
        inputMat.requireNonBinary(input)

        val targetColor = convertTo.value(current).value
        val matColor = inputMat.color

        if(matColor != targetColor) {
            val mat = uniqueVariable("${targetColor.name.lowercase()}Mat", Mat.new())

            group {
                // create mat instance variable
                private(mat)
            }

            current.scope { // add a cvtColor step in processFrame
                Imgproc("cvtColor", inputMat.value, mat, cvtColorValue(matColor, targetColor))
                output.streamIfEnabled(mat)
            }

            session.outputMatValue = GenValue.Mat(mat, targetColor) // store data in the current session
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
            return lastGenSession!!.outputMatValue
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}