package io.github.deltacv.easyvision.node.vision

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.ScalarRangeAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Core
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Mat
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Scalar
import io.github.deltacv.easyvision.codegen.build.v
import io.github.deltacv.easyvision.gui.util.ExtraWidgets
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.serialization.data.SerializeData

@RegisterNode(
    name = "nod_colorthresh",
    category = Category.COLOR_OP,
    description = "Performs a threshold in the input image and returns a binary image, discarding the pixels that were outside the range in the color space specified."
)
class ThresholdNode : DrawNode<ThresholdNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val scalar = ScalarRangeAttribute(INPUT, Colors.values()[0], "$[att_threshold]")
    val output = MatAttribute(OUTPUT, "$[att_binaryoutput]")

    override fun onEnable() {
        + input
        + scalar
        + output
    }

    @SerializeData
    private var colorValue = ImInt()

    private var lastColor = Colors.values()[0]

    override fun drawNode() {
        input.drawHere()

        ImGui.newLine()
        ImGui.text("(Enum) Color Space")

        ImGui.pushItemWidth(110.0f)
        val color = ExtraWidgets.enumCombo(Colors.values(), colorValue)
        ImGui.popItemWidth()

        ImGui.newLine()

        if(color != lastColor) {
            scalar.color = color
        }

        lastColor = color
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val range = scalar.value(current)

        var inputMat = input.value(current)
        inputMat.requireNonBinary(input)
        
        var matColor = inputMat.color
        var targetColor = lastColor

        if(matColor != targetColor) {
            if(matColor == Colors.RGBA && targetColor != Colors.RGB) {
                matColor = Colors.RGB
            } else if(matColor != Colors.RGB && targetColor == Colors.RGBA) {
                targetColor = Colors.RGB
            }
        }
        
        val needsCvt = matColor != targetColor

        val cvtMat = uniqueVariable("${targetColor.name.lowercase()}Mat", Mat.new())
        val thresholdTargetMat = uniqueVariable("${targetColor.name.lowercase()}BinaryMat", Mat.new())

        val lowerScalar = uniqueVariable("lower${targetColor.name}",
            Scalar.new(
                range.a.min.v,
                range.b.min.v,
                range.c.min.v,
                range.d.min.v,
            )
        )

        val upperScalar = uniqueVariable("upper${targetColor.name}",
            Scalar.new(
                range.a.max.v,
                range.b.max.v,
                range.c.max.v,
                range.d.max.v,
            )
        )

        // add necessary imports

        group {
            // lower color scalar
            public(lowerScalar)

            // upper color scalar
            public(upperScalar)

            if (needsCvt) {
                private(cvtMat)
            }
            // output mat target
            private(thresholdTargetMat)
        }

        current.scope {
            if(needsCvt) {
                Imgproc("cvtColor", inputMat.value, cvtMat, cvtColorValue(matColor, targetColor))
                inputMat = GenValue.Mat(cvtMat, targetColor)
            }

            Core("inRange", inputMat.value, lowerScalar, upperScalar, thresholdTargetMat)
        }

        session.outputMat = GenValue.Mat(thresholdTargetMat, targetColor, true)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return genSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}