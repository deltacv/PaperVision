package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.language.cpython.CPythonOpenCv
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType

@PaperNode(
    name = "nod_extractregion",
    category = NodeCategory.IMAGE_PROC,
    description = "des_extractregion"
)
@CodecType
class ExtractRegionNode : DrawNode<ExtractRegionNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")
    val region = RectAttribute(INPUT, "$[att_region]")
    val output = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input.rebuildOnChange()
        + region

        + output.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val inputMat = input.genValue(current)
                val regionRect = JvmOpenCv.toRectInst(region.genValue(current), current)

                val outputMat = uniqueVariable("${inputMat.value}Roi", JvmOpenCv.Mat.nullValue)

                group {
                    private(outputMat)
                }

                current.scope {
                    nameComment()
                    ifCondition(outputMat notEqualsTo nullValue) {
                        outputMat("release")
                    }

                    separate()

                    ifCondition(regionRect.value.v notEqualsTo nullValue) {
                        outputMat instanceSet inputMat.value.v.callValue("submat", JvmOpenCv.Mat, regionRect.value.v)
                        output.streamIfEnabled(outputMat, inputMat.color)
                    }
                }

                session.output = GenValue.Mat(outputMat.resolved(), inputMat.color)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val inputMat = input.genValue(current)
            val regionRect = CPythonOpenCv.toRectTuple(region.genValue(current), current)

            current {
                current.scope {
                    nameComment()

                    val rectVar = uniqueVariable("roi_rect", regionRect)
                    local(rectVar)

                    val x = rectVar[0.v, CPythonLanguage.NoType]
                    val y = rectVar[1.v, CPythonLanguage.NoType]
                    val w = rectVar[2.v, CPythonLanguage.NoType]
                    val h = rectVar[3.v, CPythonLanguage.NoType]

                    val roi = inputMat.value.v[CPythonLanguage.NoType, CPythonLanguage.sliceValue(
                        y,
                        y + h
                    ), CPythonLanguage.sliceValue(x, x + w)]

                    val roiVar = uniqueVariable("${inputMat.value.v}_roi", roi)
                    local(roiVar)

                    session.output = GenValue.Mat(roiVar.resolved(), inputMat.color)
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> GenValue.Mat.defer { current.sessionOf(this)?.output }
        else -> noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Mat
    }
}