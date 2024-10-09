package io.github.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.Range2i

class ScalarAttribute(
    mode: AttributeMode,
    color: ColorSpace,
    variableName: String? = null
) : ListAttribute(mode, IntAttribute, variableName, color.channels) {

    var color = color
        set(value) {
            fixedLength = value.channels
            field = value
        }

    override var icon = FontAwesomeIcons.GripHorizontal

    override fun drawAttributeText(index: Int, attrib: Attribute) {
        if(index < color.channelNames.size) {
            val name = color.channelNames[index]
            val elementName = name + if(name.length == 1) " " else ""

            if(attrib is TypedAttribute) {
                attrib.drawDescriptiveText = false
                attrib.inputSameLine = true
            }

            ImGui.pushFont(PaperVision.defaultImGuiFont.imfont)
            ImGui.text(elementName)
            ImGui.popFont()
        }
    }

    override fun onElementCreation(element: Attribute) {
        if(element is IntAttribute) {
            element.sliderMode(Range2i(0, 255))
        }
    }

    override fun value(current: CodeGen.Current): GenValue.Scalar {
        val values = (super.value(current) as GenValue.GList.List).elements
        val ZERO = GenValue.Int(0)

        val value = GenValue.Scalar(
            (values.getOr(0, ZERO) as GenValue.Int).value.toDouble(),
            (values.getOr(1, ZERO) as GenValue.Int).value.toDouble(),
            (values.getOr(2, ZERO) as GenValue.Int).value.toDouble(),
            (values.getOr(3, ZERO) as GenValue.Int).value.toDouble()
        )

        return value(
            current, "a Scalar", value
        ) { it is GenValue.Scalar }
    }

}