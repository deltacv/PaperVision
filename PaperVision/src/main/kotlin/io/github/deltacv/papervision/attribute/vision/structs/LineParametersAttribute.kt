package io.github.deltacv.papervision.attribute.vision.structs

import imgui.ImGui
import imgui.flag.ImGuiCol
import io.github.deltacv.papervision.action.editor.CreateLinkAction
import io.github.deltacv.papervision.action.editor.CreateNodeAction
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.AttributeType
import io.github.deltacv.papervision.attribute.TypedAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.gui.FontAwesomeIcons
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.vision.overlay.LineParametersNode

class LineParametersAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(LineParametersAttribute) {

    companion object : AttributeType {
        override val icon = FontAwesomeIcons.ChartLine

        override fun new(mode: AttributeMode, variableName: String) = LineParametersAttribute(mode, variableName)
    }

    override fun drawAfterText() {
        if(mode == AttributeMode.INPUT) {
            ImGui.sameLine()

            ImGui.pushFont(parentNode.fontAwesome.imfont)
            ImGui.pushStyleColor(ImGuiCol.Button, 0)

            if(!hasLink && ImGui.button(FontAwesomeIcons.PencilAlt)) {
                val node = parentNode.editor.addNode(LineParametersNode::class.java)

                parentNode.editor.onDraw.doOnce {
                    CreateLinkAction(
                        Link(
                            (node as LineParametersNode).output.id,
                            id
                        )
                    ).enable()

                    node.pinToMouse = true
                }
            }

            ImGui.popStyleColor()
            ImGui.popFont()
        }
    }

    override fun value(current: CodeGen.Current): GenValue {
        return if(mode == AttributeMode.INPUT && !hasLink) {
            GenValue.LineParameters.Line(
                GenValue.Scalar(0.0, 255.0, 0.0, 0.0),
                GenValue.Int(3)
            )
        } else {
            value<GenValue.LineParameters>(
                current, "a LineParameters"
            ) { it is GenValue.LineParameters }
        }
    }

}