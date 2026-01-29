package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.KeyPointAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage.NoType
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_keypointsto_rects",
    category = Category.FEATURE_DET,
    description = "des_keypointsto_rects"
)
class KeyPointsToRectsNode : DrawNode<KeyPointsToRectsNode.Session>() {

    val input = ListAttribute(INPUT, KeyPointAttribute, "$[att_keypoints]")
    val output = ListAttribute(OUTPUT, RectAttribute, "$[att_rects]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                input.requireAttachedAttribute()
                val keypoints = input.genValue(current)

                if(keypoints !is GenValue.GList.RuntimeListOf<*>) {
                    raise("Only runtime lists are supported for now")
                }

                val rects = uniqueVariable("keypointsRects", JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())

                group {
                    private(rects)
                }

                current.scope {
                    nameComment()

                    rects("clear")

                    separate()

                    foreach(AccessorVariable(JvmOpenCvTypes.KeyPoint, "kp"), keypoints.value.v.callValue("toArray", JvmOpenCvTypes.KeyPoint.arrayType())) {
                        rects("add", JvmOpenCvTypes.Rect.new(
                            it.propertyValue("center", JvmOpenCvTypes.Point),
                            JvmOpenCvTypes.Size.new(it.propertyValue("rad", FloatType), it.propertyValue("size", FloatType))
                        ))
                    }

                    session.output = GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>(rects.resolved())
                }
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val keypoints = input.genValue(current)

            if(keypoints !is GenValue.GList.RuntimeListOf<*>) {
                raise("Only runtime lists are supported for now")
            }

            // rectangles in python are just tuples of (x, y, w, h)
            current {
                val rects = uniqueVariable("keypoints_rects", NoType.newArray())

                current.scope {
                    local(rects)

                    separate()

                    foreach(DeclarableVariable(NoType, "kp"), keypoints.value.v) {
                        rects("append", CPythonLanguage.tuple(
                            it.propertyValue("pt", NoType)[0.v, NoType],
                            it.propertyValue("pt", NoType)[1.v, NoType],
                            it.propertyValue("size", NoType),
                            it.propertyValue("size", NoType)
                        ))
                    }
                }

                session.output = GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>(rects.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when (attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }
}
