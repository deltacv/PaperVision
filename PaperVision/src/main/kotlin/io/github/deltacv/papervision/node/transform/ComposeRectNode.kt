package io.github.deltacv.papervision.node.transform

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_composerect",
    category = NodeCategory.TRANSFORM,
    description = "des_composerect"
)
@CodecType
class ComposeRectNode : DrawNode<ComposeRectNode.Session>() {

    val positionAtt = Vector2Attribute(INPUT, "$[att_position]")
    val sizeAtt = Vector2Attribute(INPUT, "$[att_size]", useSizeNaming = true)

    val output = RectAttribute(OUTPUT, "$[att_rect]")

    override fun onEnable() {
        + positionAtt
        + sizeAtt
        + output
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                var positionValue = positionAtt.genValue(current)
                var sizeValue = sizeAtt.genValue(current)

                if(codeGen.isForPreviz) {
                    positionValue = JvmOpenCv.toRuntimeVec2(positionValue, current)
                    sizeValue = JvmOpenCv.toRuntimeVec2(sizeValue, current)
                }

                val (x, y) = when(positionValue) {
                    is GenValue.Vec2.Runtime -> positionValue.xValue to positionValue.yValue
                    is GenValue.Vec2.Actual -> positionValue.x to positionValue.y
                }
                val (w, h) = when(sizeValue) {
                    is GenValue.Vec2.Runtime -> sizeValue.xValue to sizeValue.yValue
                    is GenValue.Vec2.Actual -> sizeValue.x to sizeValue.y
                }

                session.rect = GenValue.Rect.Components(x, y, w, h)

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> current.nonNullSessionOf(this).rect // cannot defer rect
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("position", positionAtt)
        encoder.obj("size", sizeAtt)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("position", positionAtt)
        decoder.obj("size", sizeAtt)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var rect: GenValue.Rect.Components
    }
}