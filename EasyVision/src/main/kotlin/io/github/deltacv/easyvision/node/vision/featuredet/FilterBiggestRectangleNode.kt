package io.github.deltacv.easyvision.node.vision.featuredet

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "nod_filterbiggest_rect",
    category = Category.FEATURE_DET,
    description = "Finds all the contours (list of points) of a given binary image."
)
class FilterBiggestRectangleNode : DrawNode<FilterBiggestRectangleNode.Session>() {

    val input = ListAttribute(INPUT, RectAttribute,"Rectangles").rebuildOnChange()

    val output = RectAttribute(OUTPUT, "Biggest Rect").rebuildOnChange()

    override fun onEnable() {
        + input
        + output
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val rectsList = input.value(current)

        if(rectsList !is GenValue.GLists.RuntimeListOf<*>) {
            raise("") // TODO: Handle non-runtime lists
        }

        val biggestRect = uniqueVariable("biggestRect", OpenCvTypes.Rect.nullVal)

        group {
            private(biggestRect)
        }

        current.scope {
            foreach(variable(OpenCvTypes.Rect, "rect"), rectsList.value) {
                ifCondition(
                    biggestRect equalsTo biggestRect.nullVal or
                    (it.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                ) {
                    biggestRect instanceSet it
                }
            }
        }

        session.biggestRect = GenValue.GRects.RuntimeRect(biggestRect)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return genSession!!.biggestRect
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var biggestRect: GenValue.GRects.RuntimeRect
    }

}