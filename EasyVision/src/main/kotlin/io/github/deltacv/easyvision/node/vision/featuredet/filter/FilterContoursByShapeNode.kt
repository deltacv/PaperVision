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
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes.Size
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.util.Range2i

enum class Shape(val sides: Int?) {
    Circle(0), Triangle(3), Rectangle(4), Square(4), Polygon(null)
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

    private var previousSides = 0

    override fun onEnable() {
        + input.rebuildOnChange()

        + shape.rebuildOnChange()

        shape.onChange { refreshSidesField() }

        + sides.rebuildOnChange()

        sides.onChange {
            if (sides.thisGet() < 3  && previousSides >= 3) {
                sides.value.set(0)
                shape.currentIndex.set(Shape.Circle.ordinal)
            } else if (sides.thisGet() in 1..2) {
                sides.value.set(3)
                shape.currentIndex.set(Shape.Triangle.ordinal)
            } else {
                for ((i, shapeE) in Shape.values().withIndex()) {
                    if (sides.thisGet() == shapeE.sides) {
                        shape.currentIndex.set(i)
                        return@onChange
                    }
                }

                shape.currentIndex.set(Shape.Polygon.ordinal)
            }

            previousSides = sides.value.get()
        }

        + accuracy
        accuracy.sliderMode(Range2i(1, 100))

        + output.rebuildOnChange()

        refreshSidesField()
    }

    private var firstDraw = true

    override fun draw() {
        super.draw()

        if(firstDraw) {
            previousSides = sides.value.get()
            firstDraw = false
        }

        if(sides.thisGet() < 0) sides.value.set(0)
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputContours = input.value(current)

        if(inputContours !is GenValue.GLists.RuntimeListOf<*>){
            raise("")
        }

        val shapeValue = shape.value(current).value
        val sidesValue = sides.value(current)
        val accuracyValue = accuracy.value(current).value

        val list = uniqueVariable("filtered${shapeValue.name}Contours", JavaTypes.ArrayList(OpenCvTypes.MatOfPoint).new())

        val contours2f = uniqueVariable("contours2f", OpenCvTypes.MatOfPoint2f.new())
        val approxPolyDp = uniqueVariable("approxPolyDp", OpenCvTypes.MatOfPoint2f.new())
        val approxPolyDp2f = uniqueVariable("approxPolyDp2f", OpenCvTypes.MatOfPoint2f.new())

        group {
            private(approxPolyDp)
            private(approxPolyDp2f)
            private(contours2f)

            private(list)
        }

        current.scope {
            list("clear")

            foreach(variable(OpenCvTypes.MatOfPoint, "contour"), inputContours.value) {
                it("convertTo", contours2f, cvTypeValue("CV_32FC2"))

                separate()

                Imgproc("approxPolyDP", contours2f, approxPolyDp2f, ((double(100.0) - int(accuracyValue)) / double(100.0)) * Imgproc.callValue("arcLength", DoubleType, contours2f, trueValue), trueValue)
                approxPolyDp2f("convertTo", approxPolyDp, cvTypeValue("CV_32S"))

                separate()

                ifCondition(approxPolyDp.callValue("size", Size).propertyValue("height", IntType) equalsTo sidesValue.value.v) {
                    list("add", it)
                }

                separate()

                contours2f("release")
                approxPolyDp("release")
                approxPolyDp2f("release")
            }
        }

        session.output = GenValue.GLists.RuntimeListOf(list, GenValue.GPoints.RuntimePoints::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return lastGenSession!!.output
        }

        noValue(attrib)
    }

    private fun refreshSidesField() {
        sides.showAttributesCircles = shape.currentValue == Shape.Polygon
        sides.value.set(shape.currentValue.sides ?: if(sides.value.get() >= 5) sides.value.get() else 5)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GLists.RuntimeListOf<GenValue.GPoints.RuntimePoints>
    }

}