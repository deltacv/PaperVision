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

package io.github.deltacv.papervision.node.vision.featuredet.filter

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_filterbiggest_contour",
    category = Category.FEATURE_DET,
    description = "des_filterbiggest_contour"
)
class FilterBiggestContourNode : DrawNode<FilterBiggestContourNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val output = PointsAttribute(OUTPUT, "$[att_biggestcontour]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val contoursList = input.value(current)

                val biggestContour = uniqueVariable("biggestContour", JvmOpenCvTypes.MatOfPoint.nullVal) // TODO: huh???

                group {
                    private(biggestContour)
                }

                current.scope {
                    biggestContour instanceSet biggestContour.nullVal

                    if(contoursList is GenValue.GList.RuntimeListOf<*>) {
                        foreach(variable(JvmOpenCvTypes.MatOfPoint, "contour"), contoursList.value) { contour ->
                            val contourArea = Imgproc.callValue("contourArea", JvmOpenCvTypes.MatOfPoint, contour)
                            val biggestContourArea = Imgproc.callValue("contourArea", JvmOpenCvTypes.MatOfPoint, biggestContour)

                            ifCondition(
                                biggestContour equalsTo biggestContour.nullVal or (contourArea greaterThan biggestContourArea)
                            ) {
                                biggestContour instanceSet contour
                            }
                        }
                    } else {
                        for(element in (contoursList as GenValue.GList.ListOf<*>).elements) {
                            separate()

                            val contour = if(element is GenValue.GPoints.RuntimePoints) {
                                element.value
                            } else {
                                raise("Invalid element in contours list")
                            }

                            ifCondition(contour notEqualsTo language.nullValue) {
                                val contourArea = Imgproc.callValue("contourArea", JvmOpenCvTypes.MatOfPoint, contour)
                                val biggestContourArea = Imgproc.callValue("contourArea", JvmOpenCvTypes.MatOfPoint, biggestContour)

                                ifCondition(
                                    biggestContour equalsTo biggestContour.nullVal or (contourArea greaterThan biggestContourArea)
                                ) {
                                    biggestContour instanceSet contour
                                }
                            }
                        }
                    }
                }

                session.biggestContour = GenValue.GPoints.RuntimePoints(biggestContour)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val inputValue = input.value(current)

                current.scope {
                    val contoursList = if(inputValue is GenValue.GList.RuntimeListOf<*>) {
                        inputValue.value
                    } else {
                        val list = uniqueVariable("contours_list", CPythonLanguage.NoType.newArray())
                        local(list)

                        for(element in (inputValue as GenValue.GList.ListOf<*>).elements) {
                            if(element is GenValue.GPoints.RuntimePoints) {
                                ifCondition(element.value notEqualsTo language.nullValue) {
                                    list("append", element.value)
                                }
                            } else {
                                raise("Invalid element in contours list")
                            }
                        }

                        separate()

                        list
                    }

                    val biggestContour = uniqueVariable(
                        "biggest_contour",
                        "max".callValue(
                            CPythonLanguage.NoType,
                            contoursList,
                            CPythonLanguage.namedArgument("key", cv2.contourArea)
                        )
                    )

                    local(biggestContour)

                    session.biggestContour = GenValue.GPoints.RuntimePoints(biggestContour)
                }

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return current.sessionOf(this)!!.biggestContour
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var biggestContour: GenValue.GPoints.RuntimePoints
    }

}