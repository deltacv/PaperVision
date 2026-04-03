package org.deltacv.papervision.node.transform

import org.deltacv.papervision.attribute.AnyAttribute
import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.TypedAttribute
import org.deltacv.papervision.attribute.decomp.AttributeDecomposer
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.NoSession
import org.deltacv.papervision.codegen.dsl.generatorsBuilder
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder
import org.deltacv.papervision.serialization.v2.objTyped

@PaperNode(
    name = "nod_decomposer",
    category = NodeCategory.TRANSFORM,
    description = "des_decomposer"
)
@CodecType
class DecomposerNode : DrawNode<NoSession>() {

    var decomposer: AttributeDecomposer<*>? = null

    private var previousLinkedAttribute: Attribute? = null
    private var wasJustDecoded = false
    private var decodedWaitFrames = 0
    private var hasLoggedFirstDraw = false

    val input = AnyAttribute(INPUT, "$[att_attribute]", linkAcceptor = {
        if (it is TypedAttribute<*>) {
            val decomposer = it.attributeType.newDecomposer()

            if (decomposer != null) {
                Attribute.LinkAcceptance.Accept
            } else Attribute.LinkAcceptance.Reject("err_couldntlink_notdecomposable")
        } else Attribute.LinkAcceptance.Reject
    })

    override fun onEnable() {
        + input
        decomposer?.enable(this, input)
    }

    override fun drawNode() {
        val currentLinkedAttribute = input.availableLinkedAttribute

        if(!hasLoggedFirstDraw) {
            hasLoggedFirstDraw = true
        }

        if(wasJustDecoded) {
            if(currentLinkedAttribute != null) {
                wasJustDecoded = false
                decodedWaitFrames = 0
                previousLinkedAttribute = currentLinkedAttribute
            } else if(++decodedWaitFrames > 2) {
                wasJustDecoded = false
                decodedWaitFrames = 0
                decomposer?.disable()
                decomposer = null
            }
        } else if(currentLinkedAttribute != previousLinkedAttribute || decomposer == null) {
            decomposer?.disable()
            decomposer = null

            if(currentLinkedAttribute != null) {
                val newDecomposer = (currentLinkedAttribute as? TypedAttribute<*>)?.attributeType?.newDecomposer()

                if(newDecomposer != null) {
                    newDecomposer.enable(this, input)
                    this.decomposer = newDecomposer
                }
            }

            previousLinkedAttribute = currentLinkedAttribute
        }
    }

    override val generators = generatorsBuilder {
        generatorForAny { _, current ->
            decomposer?.let {
                current.codeGen.sessions[it] = it.genCode(input.genValue(current), current)
            }
            NoSession
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)

        decomposer?.let {
            encoder.obj("decomposer", it)
        }
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)

        decoder.obj("input", input)
        if(decoder.has("decomposer")) {
            decomposer = decoder.objTyped("decomposer")
        }

        wasJustDecoded = true
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        warnAssert(decomposer != null, "Decomposer is null")
        return decomposer?.getGenValueOf(current, attrib) ?: noValue(attrib)
    }

}
