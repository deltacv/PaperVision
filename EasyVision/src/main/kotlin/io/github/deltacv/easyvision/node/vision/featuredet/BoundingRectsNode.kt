package io.github.deltacv.easyvision.node.vision.featuredet

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
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
    category = Category.FEATURE_DET,
    description = "Calculates the bounding rectangles of a given list of points."
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>() {

    val inputContours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val outputRects   = ListAttribute(OUTPUT, RectAttribute, "$[att_boundingrects]")

    override fun onEnable() {
        + inputContours.rebuildOnChange()
        + outputRects.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputContours.value(current)

        if(input !is GenValue.GList.RuntimeListOf<*>) {
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

        session.outputRects = GenValue.GList.RuntimeListOf(rectsList, GenValue.GRect.RuntimeRect::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputRects) {
            return lastGenSession!!.outputRects
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputRects: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }

}