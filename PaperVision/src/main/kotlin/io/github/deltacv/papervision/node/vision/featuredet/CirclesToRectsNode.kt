package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.CircleAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "$[nod_circlesto_rects]",
    category = Category.FEATURE_DET,
    description = "$[des_circlesto_rects]"
)
class CirclesToRectsNode : DrawNode<CirclesToRectsNode.Session>() {

    val circles = ListAttribute(INPUT, CircleAttribute, "$[att_circles]")

    val output = ListAttribute(OUTPUT, RectAttribute, "$[att_rects]")

    override fun onEnable() {
        + circles.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val Circle = JvmOpenCvTypes.getCircleType(current)

                val circles = circles.genValue(current)
                if(circles !is GenValue.GList.RuntimeListOf<*>) {
                    raise("Only runtime lists are supported for now")
                }

                val rects = uniqueVariable("circlesRects", JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())

                group {
                    private(rects)
                }

                current.scope {
                    nameComment()

                    foreach(AccessorVariable(Circle, "circle"), circles.value.v) {
                        rects("add", JvmOpenCvTypes.Rect.new(
                            int(it.propertyValue("center", JvmOpenCvTypes.Point).propertyValue("x", DoubleType) - it.propertyValue("radius", DoubleType)),
                            int(it.propertyValue("center", JvmOpenCvTypes.Point).propertyValue("y", DoubleType) - it.propertyValue("radius", DoubleType)),
                            int(it.propertyValue("radius", DoubleType) * 2.v),
                            int(it.propertyValue("radius", DoubleType) * 2.v)
                        ))
                    }
                }

                session.outputRects = GenValue.GList.RuntimeListOf(rects.resolved(), GenValue.GRect.RuntimeRect::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val circles = circles.genValue(current)
                if(circles !is GenValue.GList.RuntimeListOf<*>) {
                    raise("Only runtime lists are supported for now")
                }

                current.scope {
                    nameComment()

                    val rects = uniqueVariable("circles_rects", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))
                    local(rects)

                    separate()

                    foreach(CPythonLanguage.accessorTupleVariable("x", "y", "r"), circles.value.v) {
                        rects("append", CPythonLanguage.tuple(
                            int(it.get("x") - it.get("r")),
                            int(it.get("y") - it.get("r")),
                            int(it.get("r") * 2.v),
                            int(it.get("r") * 2.v)
                        ))
                    }

                    session.outputRects = GenValue.GList.RuntimeListOf(rects.resolved(), GenValue.GRect.RuntimeRect::class.resolved())
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if( attrib == output) {
            return GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.outputRects }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputRects: GenValue.GList.RuntimeListOf<GenValue.GRect.RuntimeRect>
    }
}