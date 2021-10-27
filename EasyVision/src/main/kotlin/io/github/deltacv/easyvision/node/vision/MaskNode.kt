package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode

@RegisterNode(
    name = "nod_binarymask",
    category = Category.COLOR_OP,
    description = "Takes a normal image and performs a mask based on a binary image, discards or includes areas from the normal image based on the binary image."
)
class MaskNode : DrawNode<MaskNode.Session>(){

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val maskMat  = MatAttribute(INPUT, "$[att_binarymask]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat
        + maskMat

        + outputMat
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputMat.value(current)
        input.requireNonBinary(inputMat)

        val mask = maskMat.value(current)
        mask.requireBinary(maskMat)

        val output = tryName("${input.value.value!!}Mask")

        import("org.opencv.core.Core")

        group {
            private(output, new("Mat"))
        }

        current.scope {
            "$output.release"()
            "Core.bitwise_and"(input.value, input.value, output.v, mask.value)
        }

        session.outputMat = GenValue.Mat(output.v, input.color)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return genSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}