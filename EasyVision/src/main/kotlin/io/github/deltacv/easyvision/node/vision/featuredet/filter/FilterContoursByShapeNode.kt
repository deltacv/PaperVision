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
import io.github.deltacv.easyvision.codegen.build.Value
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
        val accuracyValue = accuracy.value(current).value

        val list = uniqueVariable("filtered${shapeValue.name}Contours", JavaTypes.ArrayList(OpenCvTypes.MatOfPoint).new())

        val ints = uniqueVariable("ints", OpenCvTypes.MatOfInt.new())
        val convexHull = uniqueVariable("convexHull", OpenCvTypes.MatOfPoint2f.new())
        val contours2f = uniqueVariable("contours2f", OpenCvTypes.MatOfPoint2f.new())
        val approxPolyDp = uniqueVariable("approxPolyDp", OpenCvTypes.MatOfPoint2f.new())

        group {
            private(ints)
            private(convexHull)
            private(approxPolyDp)
            private(contours2f)

            private(list)
        }

        current.scope {
            foreach(variable(OpenCvTypes.MatOfPoint, "contour"), inputContours.value) {
                Imgproc("convexHull", it, ints)

                separate()

                val arrIndex = uniqueVariable("arrIndex", ints.callValue("toArray", IntType.arrayType()))
                val arrContour = uniqueVariable("arrContour", it.callValue("toArray", OpenCvTypes.Point.arrayType()))
                val arrPoints = uniqueVariable("arrPoints", OpenCvTypes.Point.newArray(arrIndex.propertyValue("length", IntType)))

                local(arrIndex)
                local(arrContour)
                local(arrPoints)

                separate()

                forLoop(variable(IntType, "i"), int(0), arrIndex.propertyValue("length", IntType)) { i: Value ->
                    arrPoints[i] = arrContour[arrIndex[i, IntType], OpenCvTypes.Point]
                }

                separate()

                convexHull("fromArray", arrPoints)

                separate()

                contours2f("fromArray", it.callValue("toArray", OpenCvTypes.Point.arrayType()))

                Imgproc("approxPolyDp", convexHull, (double(100.0) - double(accuracyValue.toDouble())) / double(100.0) * Imgproc.callValue("arcLength", DoubleType, contours2f, trueValue))

                separate()

                ints("release")
                convexHull("release")
                contours2f("release")
                approxPolyDp("release")
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