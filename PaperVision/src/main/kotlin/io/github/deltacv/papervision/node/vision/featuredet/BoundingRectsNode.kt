package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_boundingrect",
    category = Category.FEATURE_DET,
    description = "des_boundingrect"
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

                val rectsList = uniqueVariable("${input.value.value}Rects", JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())

                group {
                    private(rectsList)
                }

                current.scope {
                    rectsList("clear")

                    foreach(variable(JvmOpenCvTypes.MatOfPoint, "points"), input.value) {
                        rectsList("add", Imgproc.callValue("boundingRect", JvmOpenCvTypes.Rect, it))
                    }
                }

                session.outputRects = GenValue.GList.RuntimeListOf(rectsList, GenValue.GRect.RuntimeRect::class)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            current {
                val session = Session()

                val input = inputContours.value(current)

                if(input !is GenValue.GList.RuntimeListOf<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val rectsList = uniqueVariable("${input.value.value}_rects", CPythonLanguage.NoType.newArray(0.v))

                current.scope {
                    local(rectsList)

                    foreach(variable(CPythonLanguage.NoType, "points"), input.value) { points ->
                        rectsList("append", cv2.callValue("boundingRect", CPythonLanguage.NoType, points))
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