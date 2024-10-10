package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_boundingrect",
    category = Category.FEATURE_DET,
    description = "Calculates the bounding rectangles of a given list of points."
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>() {

    val inputContours = ListAttribute(INPUT, PointsAttribute, "$[att_contours]")
    val outputRects   = ListAttribute(OUTPUT, RectAttribute, "$[att_boundingrects]")

    override fun onEnable() {
        + inputContours.rebuildOnChange()
        + outputRects.rebuildOnChange()
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputContours.value(current)

                if(input !is GenValue.GList.RuntimeListOf<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val rectsList = uniqueVariable("${input.value.value}Rects", JavaTypes.ArrayList(OpenCvTypes.Rect).new())

                group {
                    private(rectsList)
                }

                current.scope {
                    rectsList("clear")

                    foreach(variable(OpenCvTypes.MatOfPoint, "points"), input.value) {
                        rectsList("add", Imgproc.callValue("boundingRect", OpenCvTypes.Rect, it))
                    }
                }

                session.outputRects = GenValue.GList.RuntimeListOf(rectsList, GenValue.GRect.RuntimeRect::class)

                session
            }
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputRects) {
            return lastGenSession!!.outputRects
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputRects: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }

}