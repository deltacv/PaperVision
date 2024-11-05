package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
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
import io.github.deltacv.papervision.codegen.build.v
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_groupcontours_byarea",
    category = Category.CLASSIFICATION,
    description = "des_groupcontours_byarea"
)
class FilterContoursByAreaNode : DrawNode<FilterContoursByAreaNode.Session>() {

    val input = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")

    val minArea = IntAttribute(INPUT, "$[att_minarea]")
    val maxArea = IntAttribute(INPUT, "$[att_maxarea]")

    val output = ListAttribute(OUTPUT, PointsAttribute, "$[att_filteredcontours]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + minArea
        + maxArea

        maxArea.value.set(100)

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val contours = input.value(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minAreaVal = minArea.value(current)
            val maxAreaVal = maxArea.value(current)

            current {
                val minAreaVar = uniqueVariable("minArea", minAreaVal.value.v)
                val maxAreaVar = uniqueVariable("maxArea", maxAreaVal.value.v)

                val contoursVar = uniqueVariable("${contours.value.value}ByArea", JavaTypes.ArrayList(JvmOpenCvTypes.MatOfPoint).new())

                group {
                    public(minAreaVar, minArea.label())
                    public(maxAreaVar, maxArea.label())

                    private(contoursVar)
                }

                current.scope {
                    contoursVar("clear")

                    foreach(Variable(JvmOpenCvTypes.MatOfPoint, "contour"), contours.value) { contour ->
                        val areaVar = uniqueVariable("area", JvmOpenCvTypes.Imgproc.callValue("contourArea", DoubleType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan minAreaVar) and (areaVar lessOrEqualThan maxAreaVar)) {
                            contoursVar("add", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(contoursVar, GenValue.GPoints.RuntimePoints::class)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val contours = input.value(current)

            if(contours !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minArea = minArea.value(current).value.v
            val maxArea = maxArea.value(current).value.v

            current {
                val contoursVar = uniqueVariable("by_area_contours", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))

                current.scope {
                    local(contoursVar)

                    foreach(Variable(CPythonLanguage.NoType, "contour"), contours.value) { contour ->
                        val areaVar = uniqueVariable("area", CPythonOpenCvTypes.cv2.callValue("contourArea", CPythonLanguage.NoType, contour))
                        local(areaVar)

                        ifCondition((areaVar greaterOrEqualThan minAreaVar) and (areaVar lessOrEqualThan maxArea)) {
                            contoursVar("append", contour)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(contoursVar, GenValue.GPoints.RuntimePoints::class)
            }

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        when(attrib) {
            output -> return current.sessionOf(this)!!.output
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<*>
    }

}