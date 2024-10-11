package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.Range2i

@PaperNode(
    name = "nod_erodedilate",
    category = Category.IMAGE_PROC,
    description = "des_erodedilate"
)
class ErodeDilateNode : DrawNode<ErodeDilateNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_binaryinput]")

    val erodeValue = IntAttribute(INPUT, "$[att_erode]")
    val dilateValue = IntAttribute(INPUT, "$[att_dilate]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + erodeValue
        erodeValue.sliderMode(Range2i(0, 100))

        + dilateValue
        dilateValue.sliderMode(Range2i(0, 100))

        + outputMat.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputMat.value(current)
                input.requireBinary(inputMat)

                val erodeVal = erodeValue.value(current)
                val erodeValVariable = uniqueVariable("erodeValue", int(erodeVal.value.v))

                val dilateVal = erodeValue.value(current)
                val dilateValVariable = uniqueVariable("dilateValue", int(dilateVal.value.v))

                val element = uniqueVariable("element", JvmOpenCvTypes.Mat.nullVal)

                val output = uniqueVariable("${input.value.value!!}ErodedDilated", JvmOpenCvTypes.Mat.new())

                group {
                    public(erodeValVariable, erodeValue.label())
                    public(dilateValVariable, dilateValue.label())
                    private(element)
                    private(output)
                }

                current.scope {
                    input.value("copyTo", output)

                    ifCondition(erodeValVariable greaterThan int(0)) {
                        element instanceSet JvmOpenCvTypes.Imgproc.callValue(
                            "getStructuringElement",
                            JvmOpenCvTypes.Mat,
                            JvmOpenCvTypes.Imgproc.MORPH_RECT,
                            JvmOpenCvTypes.Size.new(erodeValVariable, erodeValVariable)
                        )

                        JvmOpenCvTypes.Imgproc("erode", output, output, element)

                        separate()

                        element("release")
                    }

                    separate()

                    ifCondition(dilateValVariable greaterThan int(0)) {
                        element instanceSet JvmOpenCvTypes.Imgproc.callValue(
                            "getStructuringElement",
                            JvmOpenCvTypes.Mat,
                            JvmOpenCvTypes.Imgproc.MORPH_RECT,
                            JvmOpenCvTypes.Size.new(dilateValVariable, dilateValVariable)
                        )

                        JvmOpenCvTypes.Imgproc("dilate", output, output, element)

                        separate()

                        element("release")
                    }

                    outputMat.streamIfEnabled(output, ColorSpace.GRAY)
                }

                session.outputMatValue = GenValue.Mat(output, input.color, input.isBinary)

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return lastGenSession!!.outputMatValue
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}