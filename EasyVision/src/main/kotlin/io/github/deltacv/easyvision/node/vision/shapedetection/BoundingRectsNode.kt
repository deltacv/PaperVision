package io.github.deltacv.easyvision.node.vision.shapedetection

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.callValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.codegen.parse.variableName
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "nod_boundingrect",
    category = Category.SHAPE_DET,
    description = "Calculates the bounding rectangles of a given list of points."
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>() {

    val inputContours = ListAttribute(INPUT, PointsAttribute, "Contours")
    val outputRects   = ListAttribute(OUTPUT, RectAttribute, "Bounding Rects")

    override fun onEnable() {
        + inputContours
        + outputRects
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputContours.value(current)

        if(input !is GenValue.GLists.RuntimeListOf<*>) {
            raise("") // TODO: Handle non-runtime lists
        }

        val rectsList = tryName("${input.value.value}Rects")

        import("org.opencv.imgproc.Imgproc")
        import("org.opencv.core.Rect")
        import("java.util.ArrayList")

        group {
            private(rectsList, new("ArrayList<Rect>"))
        }

        current.scope {
            "${rectsList}.clear"()

            foreach(variableName("MatOfPoint", "points"), input.value) {
                "${rectsList}.add"(callValue("Imgproc.boundingRect", "Rect", it))
            }
        }

        session.outputRects = GenValue.GLists.RuntimeListOf(rectsList.v, GenValue.GRects.RuntimeRect::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputRects) {
            return genSession!!.outputRects
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputRects: GenValue.GLists.RuntimeListOf<GenValue.GRects.RuntimeRect>
    }

}