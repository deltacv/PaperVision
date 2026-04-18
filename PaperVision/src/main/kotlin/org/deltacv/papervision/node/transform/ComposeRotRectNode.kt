package org.deltacv.papervision.node.transform

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import org.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.resolve.resolved
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

    override val generators = polyglot {
        generatorForAny {
            current {
                val session = Session()

                val positionValue = positionAtt.genValue(current)
                val sizeValue = sizeAtt.genValue(current)
                val angleValue = angleAtt.genValue(current)

                if(codeGen.isForPreviz) {
                    val xVar = uniqueVariable("rotRectX", positionValue.x.v)
                    val yVar = uniqueVariable("rotRectY", positionValue.y.v)
                    val wVar = uniqueVariable("rotRectW", sizeValue.x.v)
                    val hVar = uniqueVariable("rotRectH", sizeValue.y.v)
                    val angleVar = uniqueVariable("rotRectAngle", angleValue.v)

                    group {
                        public(xVar, positionAtt.tunerLabel(0))
                        public(yVar, positionAtt.tunerLabel(1))
                        public(wVar, sizeAtt.tunerLabel(0))
                        public(hVar, sizeAtt.tunerLabel(1))
                        public(angleVar, angleAtt.tunerLabel())
                    }

                    session.rotRect = GenValue.RotatedRect.Components(
                        GenValue.Double.Runtime(xVar.resolved()),
                        GenValue.Double.Runtime(yVar.resolved()),
                        GenValue.Double.Runtime(wVar.resolved()),
                        GenValue.Double.Runtime(hVar.resolved()),
                        GenValue.Double.Runtime(angleVar.resolved())
                    )
                } else {
                    session.rotRect = GenValue.RotatedRect.Components(
                        positionValue.x.toDouble(current), positionValue.y.toDouble(current),
                        sizeValue.x.toDouble(current), sizeValue.y.toDouble(current),
                        angleValue
                    )
                }

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