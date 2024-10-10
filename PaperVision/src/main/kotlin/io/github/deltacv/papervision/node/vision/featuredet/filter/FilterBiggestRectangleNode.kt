package io.github.deltacv.papervision.node.vision.featuredet.filter

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_filterbiggest_rect",
    category = Category.FEATURE_DET,
    description = "Finds all the contours (list of points) of a given binary image."
)
class FilterBiggestRectangleNode : DrawNode<FilterBiggestRectangleNode.Session>() {

    val input = ListAttribute(INPUT, RectAttribute, "$[att_rects]")
    val output = RectAttribute(OUTPUT, "$[att_biggestrect]")

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

                val biggestRect = uniqueVariable("biggestRect", OpenCvTypes.Rect.nullVal)

                group {
                    private(biggestRect)
                }

                current.scope {
                    biggestRect instanceSet biggestRect.nullVal

                    foreach(variable(OpenCvTypes.Rect, "rect"), rectsList.value) { rect ->
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