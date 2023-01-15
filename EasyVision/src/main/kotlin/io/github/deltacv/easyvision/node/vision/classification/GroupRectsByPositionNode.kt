package io.github.deltacv.easyvision.node.vision.classification

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.util.Range2i

enum class Orientation { Vertical, Horizontal }

@RegisterNode(
    name = "nod_grouprects_byposition",
    category = Category.CLASSIFICATION,
    description = "Finds all the contours (list of points) of a given binary image."
)
class GroupRectsByPositionNode : DrawNode<GroupRectsByPositionNode.Session>() {

    val input = ListAttribute(INPUT, RectAttribute,"$[att_rects]")

    val areas = IntAttribute(INPUT, "$[att_areas]")
    val areasOrientation = EnumAttribute(INPUT, Orientation.values(), "$[att_areasorientation]")
    val areasOffsets = ListAttribute(INPUT, IntAttribute, "$[att_areasspacing]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + areas.rebuildOnChange()
        areas.normalMode(Range2i(1, 5))

        + areasOrientation.rebuildOnChange()
        + areasOffsets.rebuildOnChange()

        areasOffsets.fixedLength = 1
        areas.onChange {
            areasOffsets.fixedLength = areas.thisGet()

            areasOffsets.forEach<IntAttribute> { attr ->
                attr.normalMode(Range2i(0, Int.MAX_VALUE))
            }


        }

        //+ output.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GLists.RuntimeListOf<GenValue.GPoints.RuntimePoints>
    }

}