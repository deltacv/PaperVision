package io.github.deltacv.papervision.attribute

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.util.FontAwesomeIcons
import io.github.deltacv.papervision.serialization.v2.CodecType

@CodecType(instantiable = false)
class AnyAttribute(
    override val mode: AttributeMode,
    override var variableName: String?,
    private val linkAcceptor: (Attribute) -> LinkAcceptance = { LinkAcceptance.Accept }
) : TypedAttribute<GenValue>(Companion) {

    companion object: AttributeType<AnyAttribute> {
        override val icon = FontAwesomeIcons.Asterisk
        override fun new(mode: AttributeMode, variableName: String) = AnyAttribute(mode, variableName)
    }

    override fun genValue(current: CodeGen.Current): GenValue = readGenValue(current)

    override fun acceptLink(other: Attribute) = linkAcceptor(other)
}