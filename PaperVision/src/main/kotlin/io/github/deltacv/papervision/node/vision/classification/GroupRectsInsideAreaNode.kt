package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.util.Range2i

enum class Orientation { Vertical, Horizontal }

@PaperNode(
    name = "nod_grouprects_insidearea",
    category = Category.CLASSIFICATION,
    description = "Finds all the contours (list of points) of a given binary image."
)
class GroupRectsInsideAreaNode : DrawNode<GroupRectsInsideAreaNode.Session>() {

    val source = MatAttribute(INPUT,"$[att_source]")
    val input = ListAttribute(INPUT, RectAttribute, "$[att_rects]")

    val areaOrientation = EnumAttribute(INPUT, Orientation.values(), "$[att_areaorientation]")

    val areaStartPositionPercentage = IntAttribute(INPUT, "$[att_areastart_positionpercentage]")
    val areaEndPositionPercentage = IntAttribute(INPUT, "$[att_areaend_positionpercentage]")

    val areaRect = RectAttribute(OUTPUT, "$[att_arearect]")
    val output = ListAttribute(OUTPUT, RectAttribute, "$[att_rectsinside]")

    override fun onEnable() {
        + source.rebuildOnChange()
        + input.rebuildOnChange()

        + areaOrientation.rebuildOnChange()
        + areaStartPositionPercentage.rebuildOnChange()
        areaStartPositionPercentage.sliderMode(Range2i(1, 100))

        + areaEndPositionPercentage.rebuildOnChange()
        areaEndPositionPercentage.sliderMode(Range2i(1, 100))

        + areaRect.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val sourceValue = source.value(current).value
        val rectsList = input.value(current)

        if(rectsList !is GenValue.GList.RuntimeListOf<*>) {
            raise("") // TODO: Handle non-runtime lists
        }

        val areaOrientationValue = areaOrientation.value(current)

        val startPositionPerc = int(areaStartPositionPercentage.value(current).value)
        val endPositionPerc = int(areaEndPositionPercentage.value(current).value)

        val groupedRects = uniqueVariable("rectsInside${areaOrientationValue.value.name}Area", JavaTypes.ArrayList(OpenCvTypes.Rect).new())

        val areaRectVariable = uniqueVariable("areaRect", OpenCvTypes.Rect.new())

        group {
            private(groupedRects)

            if(areaRect.hasLink) {
                private(areaRectVariable)
            }
        }

        current.scope {
            groupedRects("clear")

            separate()

            val imageSize = sourceValue.callValue("size", OpenCvTypes.Size)

            val start = when(areaOrientationValue.value) {
                Orientation.Vertical -> uniqueVariable(
                    "start",
                    imageSize.propertyValue("width", IntType) * (startPositionPerc / double(100.0))
                )

                Orientation.Horizontal -> uniqueVariable(
                    "start",
                    imageSize.propertyValue("height", IntType) * (startPositionPerc / double(100.0))
                )
            }

            val end = when(areaOrientationValue.value) {
                Orientation.Vertical -> uniqueVariable(
                    "end",
                    imageSize.propertyValue("width", IntType) * (endPositionPerc / double(100.0))
                )

                Orientation.Horizontal -> uniqueVariable(
                    "end",
                    imageSize.propertyValue("height", IntType) * (endPositionPerc / double(100.0))
                )
            }

            local(start)
            local(end)

            separate()

            foreach(Variable(OpenCvTypes.Rect, "rect"), rectsList.value) {
                when(areaOrientationValue.value) {
                    Orientation.Horizontal -> {
                        ifCondition((it.propertyValue("y", IntType) greaterOrEqualThan start) and (it.propertyValue("y", IntType) + it.propertyValue("height", IntType) lessOrEqualThan end)) {
                            groupedRects("add", it)
                        }
                    }

                    Orientation.Vertical -> {
                        ifCondition((it.propertyValue("x", IntType) greaterOrEqualThan start) and (it.propertyValue("x", IntType) + it.propertyValue("width", IntType) lessOrEqualThan end)) {
                            groupedRects("add", it)
                        }
                    }
                }
            }

            if(areaRect.hasLink) {
                when(areaOrientationValue.value) {
                    Orientation.Horizontal -> {
                        areaRectVariable.propertyVariable("x", IntType) set int(start)
                        areaRectVariable.propertyVariable("y", IntType) set int(0)
                        areaRectVariable.propertyVariable("width", IntType) set int(end - start)
                        areaRectVariable.propertyVariable("height", IntType) set int(imageSize.propertyValue("height", IntType))
                    }

                    Orientation.Vertical -> {
                        areaRectVariable.propertyVariable("x", IntType) set int(0)
                        areaRectVariable.propertyVariable("y", IntType) set int(start)
                        areaRectVariable.propertyVariable("width", IntType) set int(imageSize.propertyValue("width", IntType))
                        areaRectVariable.propertyVariable("height", IntType) set int(end - start)
                    }
                }
            }
        }

        session.areaRectOutput = GenValue.GRect.RuntimeRect(areaRectVariable)
        session.output = GenValue.GList.RuntimeListOf(groupedRects, GenValue.GRect.RuntimeRect::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == areaRect) {
            return lastGenSession!!.areaRectOutput!!
        } else if(attrib == output) {
            return lastGenSession!!.output
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        var areaRectOutput: GenValue.GRect.RuntimeRect? = null
        lateinit var output: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }

}