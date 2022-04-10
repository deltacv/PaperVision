package io.github.deltacv.easyvision.node.vision.imageproc

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Mat
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Size
import io.github.deltacv.easyvision.codegen.build.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.imageproc.BlurAlgorithm.*

enum class BlurAlgorithm { Gaussian, Box, Median, Bilateral }

@RegisterNode(
    name = "nod_blur",
    category = Category.IMAGE_PROC,
    description = "Takes a normal image and performs a mask based on a binary image, discards or includes areas from the normal image based on the binary image."
)
class BlurNode : DrawNode<BlurNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]").rebuildOnChange()

    val blurAlgo = EnumAttribute(INPUT, values(), "$[att_bluralgo]")
    val blurValue = IntAttribute(INPUT, "$[att_value]")

    val output = MatAttribute(OUTPUT, "$[att_output]").enablePrevizButton()

    override fun onEnable() {
        + input.rebuildOnChange()
        + blurAlgo
        + blurValue
        + output
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputMat = input.value(current)
        val algo = blurAlgo.value(current).value
        val blurVal = blurValue.value(current)

        val blurValVariable = uniqueVariable("blurValue", int(blurVal.value.v))
        val outputMat = uniqueVariable("blur${algo.name}Mat", Mat.new())

        group {
            public(blurValVariable, blurValue.label())
            private(outputMat)
        }

        current.scope {
            when(algo) {
                Gaussian -> {
                    val kernelSize = 6.v * blurValVariable + 1.v
                    val sizeBlurVal = Size.new(kernelSize, kernelSize)

                    Imgproc("GaussianBlur", inputMat.value, outputMat, sizeBlurVal, blurValVariable)
                }
                Box -> {
                    val kernelSize = 2.v * blurValVariable + 1.v
                    val sizeBlurVal = Size.new(kernelSize, kernelSize)

                    Imgproc("blur", inputMat.value, outputMat, sizeBlurVal)
                }
                Median -> {
                    val kernelSize = 2.v * blurValVariable + 1.v
                    Imgproc("medianBlur", inputMat.value, outputMat, kernelSize)
                }
                Bilateral -> {
                    Imgproc("bilateralFilter", inputMat.value, outputMat, (-1).v, blurValVariable, blurValVariable)
                }
            }
        }

        session.outputMatValue = GenValue.Mat(outputMat, inputMat.color)

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