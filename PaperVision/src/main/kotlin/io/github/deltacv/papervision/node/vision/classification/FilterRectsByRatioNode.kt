package io.github.deltacv.papervision.node.vision.classification

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.Variable
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
    name = "nod_grouprects_byratio",
    category = Category.CLASSIFICATION,
    description = "des_grouprects_byratio"
)
class FilterRectsByRatioNode : DrawNode<FilterRectsByRatioNode.Session>() {

    val input = ListAttribute(INPUT, RectAttribute, "$[att_rects]")

    val minRatio = IntAttribute(INPUT, "$[att_minratio]")
    val maxRatio = IntAttribute(INPUT, "$[att_maxratio]")

    val output = ListAttribute(OUTPUT, RectAttribute, "$[att_filteredrects]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + minRatio
        + maxRatio

        minRatio.value.set(0)
        maxRatio.value.set(100)

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            val rects = input.genValue(current)

            if(rects !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input contours must be a runtime list") // TODO: support other types
            }

            val minRatioVal = minRatio.genValue(current)
            val maxRatioVal = maxRatio.genValue(current)

            current {
                val minRatioVar = uniqueVariable("minRatio", minRatioVal.value.v)
                val maxRatioVar = uniqueVariable("maxRatio", maxRatioVal.value.v)

                val rectsVar = uniqueVariable("${rects.value.v}ByRatio", JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())

                group {
                    public(minRatioVar, minRatio.label())
                    public(maxRatioVar, maxRatio.label())

                    private(rectsVar)
                }

                current.scope {
                    nameComment()

                    rectsVar("clear")

                    foreach(Variable(JvmOpenCvTypes.Rect, "rect"), rects.value.v) { rect ->
                        val ratioVar = uniqueVariable("ratio", rect.propertyValue("height", IntType).castTo(DoubleType) / rect.propertyValue("width", IntType).castTo(DoubleType))
                        local(ratioVar)

                        ifCondition((ratioVar greaterOrEqualThan (minRatioVar / 100.0.v)) and (ratioVar lessOrEqualThan (maxRatioVar / 100.0.v))) {
                            rectsVar("add", rect)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(rectsVar.resolved(), GenValue.GRect.RuntimeRect::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val rects = input.genValue(current)

            if(rects !is GenValue.GList.RuntimeListOf<*>) {
                raise("Input must be a runtime list") // TODO: support other types
            }

            val minRatioVal = minRatio.genValue(current)
            val maxRatioVal = maxRatio.genValue(current)

            current {
                val rectsVar = uniqueVariable("${rects.value.v}_by_ratio", CPythonLanguage.newArrayOf(CPythonLanguage.NoType))

                current.scope {
                    nameComment()

                    local(rectsVar)

                    separate()

                    foreach(Variable(CPythonLanguage.NoType, "rect"), rects.value.v) { rect ->
                        val ratioVar = uniqueVariable("ratio", (rect[2.v, IntType] / rect[3.v, IntType]))
                        local(ratioVar)

                        ifCondition((ratioVar greaterOrEqualThan (minRatioVal.value.v / 100.0.v)) and (ratioVar lessOrEqualThan (maxRatioVal.value.v / 100.0.v))) {
                            rectsVar("append", rect)
                        }
                    }
                }

                session.output = GenValue.GList.RuntimeListOf(rectsVar.resolved(), GenValue.GPoints.RuntimePoints::class.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<*>
    }

}