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
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

enum class BoundingMode {
    Normal, Rotated
}

@PaperNode(
    name = "nod_groupcontours_byratio",
    category = Category.CLASSIFICATION,
    description = "des_groupcontours_byratio"
)
class FilterContoursByRatioNode : DrawNode<FilterContoursByRatioNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val boundingMode = EnumAttribute(INPUT, BoundingMode.values(), "$[att_boundingmode]")

    val minRatio = IntAttribute(INPUT, "$[att_minratio]")
    val maxRatio = IntAttribute(INPUT, "$[att_maxratio]")

    val output = ListAttribute(OUTPUT, PointsAttribute, "$[att_filteredcontours]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + boundingMode.rebuildOnChange()

        + minRatio
        + maxRatio

        minRatio.value.set(0)
        maxRatio.value.set(100)

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val contours = input.value(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minRatioVal = minRatio.value(current)
            val maxRatioVal = maxRatio.value(current)
            val mode = boundingMode.value(current).value

            current {
                val minRatioVar = uniqueVariable("minRatio", minRatioVal.value.v)
                val maxRatioVar = uniqueVariable("maxRatio", maxRatioVal.value.v)

                val contoursVar = uniqueVariable("${contours.value.value}ByRatio", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())

                val points2f = uniqueVariable("${contours.value.value ?: "points"}2f", JvmOpenCvTypes.MatOfPoint2f.new())

                group {
                    public(minRatioVar, minRatio.label())
                    public(maxRatioVar, maxRatio.label())

                    private(contoursVar)

                    if(mode == BoundingMode.Rotated) {
                        private(points2f)
                    }
                }

                current.scope {
                    nameComment()

                    contoursVar("clear")

                    foreach(Variable(JvmOpenCvTypes.MatOfPoint, "contour"), contours.value.v) { contour ->
                        val ratioVar = if(mode == BoundingMode.Normal) {
                            val rect = uniqueVariable("rect", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, contour))
                            local(rect)

                            uniqueVariable("ratio", rect.propertyValue("height", IntType).castTo(DoubleType) / rect.propertyValue("width", IntType).castTo(DoubleType))
                        } else {
                            points2f("release")
                            contour("convertTo", points2f, cvTypeValue("CV_32F"))

                            val rect = uniqueVariable("rect", JvmOpenCvTypes.Imgproc.callValue("minAreaRect", JvmOpenCvTypes.RotatedRect, points2f))
                            local(rect)

                            separate()

                            val width = uniqueVariable("width", rect.propertyValue("size", JvmOpenCvTypes.Size).propertyValue("width", IntType).castTo(DoubleType))
                            val height = uniqueVariable("height", rect.propertyValue("size", JvmOpenCvTypes.Size).propertyValue("height", IntType).castTo(DoubleType))

                            local(width)
                            local(height)

                            ifCondition(height greaterThan width) {
                                val temp = uniqueVariable("temp", width)
                                local(temp)

                                width set height
                                height set temp
                            }

                            uniqueVariable("ratio", height / width)
                        }

                        separate()

                        local(ratioVar)

                        separate()

                        ifCondition((ratioVar greaterOrEqualThan (minRatioVar / 100.0.v)) and (ratioVar lessOrEqualThan (maxRatioVar / 100.0.v))) {
                            contoursVar("add", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(contoursVar.resolved(), GenValue.GPoints.RuntimePoints::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val contours = input.value(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minRatioVal = minRatio.value(current)
            val maxRatioVal = maxRatio.value(current)

            current {
                val contoursVar = uniqueVariable("${contours.value.value}_by_ratio", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))

                current.scope {
                    local(contoursVar)

                    separate()

                    foreach(Variable(CPythonLanguage.NoType, "contour"), contours.value.v) { contour ->
                        val rectangle = CPythonLanguage.tupleVariables(
                            CPythonOpenCvTypes.cv2.callValue("boundingRect", CPythonLanguage.NoType, contour), // "rect" is a tuple of 4 values:
                            "x", "y", "w", "h"
                        )
                        local(rectangle)

                        val ratioVar = uniqueVariable("ratio", (rectangle.get("w") / rectangle.get("h")))
                        local(ratioVar)

                        separate()

                        ifCondition((ratioVar greaterOrEqualThan (minRatioVal.value.v / 100.0.v)) and (ratioVar lessOrEqualThan (maxRatioVal.value.v / 100.0.v))) {
                            contoursVar("append", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(contoursVar.resolved(), GenValue.GPoints.RuntimePoints::class.resolved())
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<*>
    }

}