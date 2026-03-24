package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
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
        + region.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val inputMat = input.genValue(current)
                val regionRect = JvmOpenCv.toRectInst(region.genValue(current), current)

                val outputMat = uniqueVariable("${inputMat.value}Cropped", JvmOpenCv.Mat.nullValue)

                group {
                    private(outputMat)
                }

                current.scope {
                    nameComment()

                    outputMat instanceSet inputMat.value.v.callValue("submat", JvmOpenCv.Mat, regionRect.value.v)
                }
            }

            session
        }
    }

    class Session : CodeGenSession {

    }
}