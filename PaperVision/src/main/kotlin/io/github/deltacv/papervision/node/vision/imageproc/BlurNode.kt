package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Size
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.imageproc.BlurAlgorithm.*

enum class BlurAlgorithm { Gaussian, Box, Median, Bilateral }

@PaperNode(
    name = "nod_blur",
    category = Category.IMAGE_PROC,
    description = "des_blur"
)
class BlurNode : DrawNode<BlurNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    val blurAlgo = EnumAttribute(INPUT, values(), "$[att_bluralgo]")
    val blurValue = IntAttribute(INPUT, "$[att_value]")

    val output = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + blurAlgo
        + blurValue
        + output.enablePrevizButton()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
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


                    output.streamIfEnabled(outputMat, inputMat.color)
                }

                session.outputMatValue = GenValue.Mat(outputMat, inputMat.color)

                session
            }
        }
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