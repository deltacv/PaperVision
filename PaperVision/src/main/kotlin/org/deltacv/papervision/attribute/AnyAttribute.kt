package org.deltacv.papervision.attribute

import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.gui.font.FontAwesomeIcons
import org.deltacv.papervision.serialization.v2.CodecType

@CodecType(instantiable = false)
class AnyAttribute(
    override val mode: AttributeMode,
    override var attributeName: String?,
    private val linkAcceptor: (Attribute) -> LinkAcceptance = { LinkAcceptance.Accept }
) : TypedAttribute<GenValue>(Companion) {

    companion object: AttributeType<AnyAttribute> {
        override val icon = FontAwesomeIcons.Asterisk
        override fun new(mode: AttributeMode, variableName: String) = AnyAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current): GenValue = readGenValue(current)

    override fun acceptLink(other: Attribute) = linkAcceptor(other)
}



