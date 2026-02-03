/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.EnumAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Size
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.util.Range2i

enum class Shape(val sides: Int?) {
    Circle(0), Triangle(3), Rectangle(4), Square(4), Polygon(null)
}

@PaperNode(
    name = "nod_groupcontours_byshape",
    category = Category.CLASSIFICATION,
    description = "des_groupcontours_byshape"
)
class FilterContoursByShapeNode : DrawNode<FilterContoursByShapeNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val shape = EnumAttribute(INPUT, Shape.values(), "$[att_shape]")
    val sides = IntAttribute(INPUT, "$[att_sides]")

    val accuracy = IntAttribute(INPUT, "$[att_accuracy]")

    val output = ListAttribute(OUTPUT, PointsAttribute, "$[att_filteredcontours]")

    private var previousSides = 0

    override fun onEnable() {
        + input.rebuildOnChange()

        + shape.rebuildOnChange()

        shape.onChange { refreshSidesField() }

        + sides.rebuildOnChange()

        sides.onChange {
            if (sides.readEditorValue() < 3  && previousSides >= 3) {
                sides.value.set(0)
                shape.currentIndex.set(Shape.Circle.ordinal)
            } else if (sides.readEditorValue() in 1..2) {
                sides.value.set(3)
                shape.currentIndex.set(Shape.Triangle.ordinal)
            } else {
                for ((i, shapeE) in Shape.values().withIndex()) {
                    if (sides.readEditorValue() == shapeE.sides) {
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

        if(sides.readEditorValue() < 0) sides.value.set(0)
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val inputContours = input.genValue(current)

                if(inputContours !is GenValue.GList.RuntimeListOf<*>){
                    raise("")
                }

                val shapeValue = shape.genValue(current).value
                val sidesValue = sides.genValue(current)
                val accuracyValue = accuracy.genValue(current).value

                val list = uniqueVariable("filtered${shapeValue.name}Contours", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())

                val contours2f = uniqueVariable("contours2f", JvmOpenCvTypes.MatOfPoint2f.new())
                val approxPolyDp = uniqueVariable("approxPolyDp", JvmOpenCvTypes.MatOfPoint2f.new())
                val approxPolyDp2f = uniqueVariable("approxPolyDp2f", JvmOpenCvTypes.MatOfPoint2f.new())

                group {
                    private(approxPolyDp)
                    private(approxPolyDp2f)
                    private(contours2f)

                    private(list)
                }

                current.scope {
                    nameComment()

                    list("clear")

                    foreach(variable(JvmOpenCvTypes.MatOfPoint, "contour"), inputContours.value.v) {
                        it("convertTo", contours2f, cvTypeValue("CV_32FC2"))

                        separate()

                        Imgproc("approxPolyDP", contours2f, approxPolyDp2f, ((double(100.0) - int(accuracyValue.v)) / double(100.0)) * Imgproc.callValue("arcLength", DoubleType, contours2f, trueValue), trueValue)
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

                session.output = GenValue.GList.RuntimeListOf(list.resolved(), GenValue.GPoints.RuntimePoints::class.resolved())

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
        }

        noValue(attrib)
    }

    private fun refreshSidesField() {
        sides.showAttributesCircles = shape.currentValue == Shape.Polygon
        sides.value.set(shape.currentValue.sides ?: if(sides.value.get() >= 5) sides.value.get() else 5)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<GenValue.GPoints.RuntimePoints>
    }

}
