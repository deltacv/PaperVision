package io.github.deltacv.papervision.node.vision.featuredet.filter

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generators
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

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val rectsList = input.value(current)

                if(rectsList !is GenValue.GList.RuntimeListOf<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val biggestRect = uniqueVariable("biggestRect", JvmOpenCvTypes.Rect.nullVal)

                group {
                    private(biggestRect)
                }

                current.scope {
                    biggestRect instanceSet biggestRect.nullVal

                    foreach(variable(JvmOpenCvTypes.Rect, "rect"), rectsList.value) { rect ->
                        ifCondition(
                            biggestRect equalsTo biggestRect.nullVal or
                                    (rect.callValue("area", DoubleType) greaterThan biggestRect.callValue("area", DoubleType))
                        ) {
                            biggestRect instanceSet rect
                        }
                    }
                }

                session.biggestRect = GenValue.GRect.RuntimeRect(biggestRect)

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return lastGenSession!!.biggestRect
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var biggestRect: GenValue.GRect.RuntimeRect
    }

}