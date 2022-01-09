package io.github.deltacv.easyvision.node.vision.shapedetection

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "nod_boundingrect",
    category = Category.SHAPE_DET,
    description = "Calculates the bounding rectangles of a given list of points."
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>() {

    val inputContours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val outputRects   = ListAttribute(OUTPUT, RectAttribute, "$[att_boundingrects]")

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

        val rectsList = uniqueVariable("${input.value.value}Rects", JavaTypes.ArrayList(OpenCvTypes.Rect).new())

        group {
            private(rectsList)
        }

        current.scope {
            rectsList("clear")

            foreach(variable(OpenCvTypes.MatOfPoint, "points"), input.value) {
                rectsList("add", Imgproc.callValue("boundingRect", OpenCvTypes.Rect, it))
            }
        }

        session.outputRects = GenValue.GLists.RuntimeListOf(rectsList, GenValue.GRects.RuntimeRect::class)

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