package io.github.deltacv.easyvision.node.vision.featuredet.filter

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.rebuildOnChange
import io.github.deltacv.easyvision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.build.ConValue
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.util.Range2i

enum class Shape(val sides: Int?) {
    Triangle(3), Rectangle(4), Square(4), Polygon(null)
}

@RegisterNode(
    name = "nod_filtercontours_byshape",
    category = Category.FEATURE_DET,
    description = "Finds all the contours (list of points) of a given binary image."
)
class FilterContoursByShapeNode : DrawNode<FilterContoursByShapeNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute,"$[att_contours]")

    val shape = EnumAttribute(INPUT, Shape.values(), "$[att_shape]")
    val sides = IntAttribute(INPUT, "$[att_sides]")

    val accuracy = IntAttribute(INPUT, "$[att_accuracy]")

    val output = ListAttribute(OUTPUT, PointsAttribute,"$[att_filteredcontours]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + shape.rebuildOnChange()

        shape.onChange { refreshSidesField() }

        + sides.rebuildOnChange()

        sides.onChange {
            for((i, shapeE) in Shape.values().withIndex()) {
                if(sides.thisGet() == shapeE.sides) {
                    shape.currentIndex.set(i)
                    return@onChange
                }
            }

            shape.currentIndex.set(Shape.Polygon.ordinal)
        }

        + accuracy
        accuracy.sliderMode(Range2i(0, 100))

        + output.rebuildOnChange()

        refreshSidesField()
    }

    override fun draw() {
        super.draw()

        if(sides.thisGet() < 3) sides.value.set(3)
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputContours = input.value(current)

        if(inputContours !is GenValue.GLists.RuntimeListOf<*>){
            raise("")
        }

        val shapeValue = shape.value(current).value
        val sidesValue = sides.value(current)

        val list = uniqueVariable("filtered${shapeValue.name}Contours", JavaTypes.ArrayList(OpenCvTypes.MatOfPoint).new())

        group {
            private(list)
        }

        current.scope {
            val convexHull = uniqueVariable("convexHull", OpenCvTypes.MatOfPoint2f.new())
            local(convexHull)

            val contours2f = uniqueVariable("matOfPoint2f", OpenCvTypes.MatOfPoint2f.new())
            local(contours2f)

            val approxPolyDp = uniqueVariable("approxPolyDp", OpenCvTypes.MatOfPoint2f.new())
            local(approxPolyDp)

            val ints = uniqueVariable("ints", OpenCvTypes.MatOfInt.new())
            local(ints)

            foreach(variable(OpenCvTypes.MatOfPoint, "contour"), inputContours.value) {
                Imgproc("convexHull", it, ints)

                val arrIndex = uniqueVariable("arrIndex", ints.callValue("toArray", IntType.arrayType()))
                val arrContour = uniqueVariable("arrContour", it.callValue("toArray", OpenCvTypes.Point.arrayType()))
                val arrPoints = uniqueVariable("arrPoints", OpenCvTypes.Point.newArray(arrIndex["length", IntType]))

                group {
                    local(arrIndex)
                    local(arrContour)
                    local(arrPoints)
                }

                group {
                    convexHull("fromArray", arrPoints)
                }

                group {
                    convexHull("release")
                    contours2f("release")
                    approxPolyDp("release")
                    ints("release")
                }
            }
        }

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return GenValue.GLists.RuntimeListOf(ConValue(OpenCvTypes.MatOfPoint, "shaped"), GenValue.GPoints.Points::class)
        }

        noValue(attrib)
    }

    private fun refreshSidesField() {
        sides.showAttributesCircles = shape.currentValue == Shape.Polygon
        sides.value.set(shape.currentValue.sides ?: 5)
    }

    class Session : CodeGenSession {
    }

}