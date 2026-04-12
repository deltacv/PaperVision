package org.deltacv.papervision.node.transform

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import org.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder
import org.deltacv.papervision.serialization.v2.objOrSkip

@PaperNode(
    name = "nod_composerot_rect",
    category = NodeCategory.TRANSFORM,
    description = "des_composerot_rect"
)
@CodecType
class ComposeRotRectNode : DrawNode<ComposeRotRectNode.Session>() {

    val positionAtt = Vector2Attribute(INPUT, "$[att_position]")
    val sizeAtt = Vector2Attribute(INPUT, "$[att_size]", useSizeNaming = true)
    val angleAtt = DoubleAttribute(INPUT, "$[att_angle]")

    val output = RotatedRectAttribute(OUTPUT, "$[att_rotrect]")

    override fun onEnable() {
        + positionAtt
        + sizeAtt
        + angleAtt
        + output
    }

    override val generators = generatorsBuilder {
        generatorForAny {
            current {
                val session = Session()

                val positionValue = positionAtt.genValue(current)
                val sizeValue = sizeAtt.genValue(current)
                val angleValue = angleAtt.genValue(current)

                val (x, y) = when(positionValue) {
                    is GenValue.Vec2.Runtime -> positionValue.xValue.toDouble(current) to positionValue.yValue.toDouble(current)
                    is GenValue.Vec2.Actual -> positionValue.x.toDouble(current) to positionValue.y.toDouble(current)
                }
                
                val (w, h) = when(sizeValue) {
                    is GenValue.Vec2.Runtime -> sizeValue.xValue.toDouble(current) to sizeValue.yValue.toDouble(current)
                    is GenValue.Vec2.Actual -> sizeValue.x.toDouble(current) to sizeValue.y.toDouble(current)
                }

                session.rotRect = GenValue.RotatedRect.Components(x, y, w, h, angleValue)

                session
            }
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> current.nonNullSessionOf(this).rotRect
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("position", positionAtt)
        encoder.obj("size", sizeAtt)
        encoder.obj("angle", angleAtt)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.objOrSkip("position", positionAtt)
        decoder.objOrSkip("size", sizeAtt)
        decoder.objOrSkip("angle", angleAtt)
        decoder.objOrSkip("output", output)
    }

    class Session : CodeGenSession {
        lateinit var rotRect: GenValue.RotatedRect.Components
    }
}