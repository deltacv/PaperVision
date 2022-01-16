package io.github.deltacv.easyvision.attribute.math

import imgui.type.ImInt
import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.attribute.AttributeMode
import io.github.deltacv.easyvision.attribute.Type
import io.github.deltacv.easyvision.attribute.TypedAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.gui.util.ExtraWidgets
import io.github.deltacv.easyvision.serialization.data.SerializeData
import java.util.*

class RangeAttribute(
    override val mode: AttributeMode,
    override var variableName: String? = null
) : TypedAttribute(Companion) {

    companion object : Type {
        override val name = "Range"

        override fun new(mode: AttributeMode, variableName: String) = RangeAttribute(mode, variableName)
    }

    var min = 0
    var max = 255

    @SerializeData
    val minValue = ImInt(min)
    @SerializeData
    val maxValue = ImInt(max)

    private var prevMin: Int? = null
    private var prevMax: Int? = null

    private val minId by EasyVision.miscIds.nextId()
    private val maxId by EasyVision.miscIds.nextId()

    override fun drawAttribute() {
        super.drawAttribute()

        if(!hasLink) {
            sameLineIfNeeded()

            ExtraWidgets.rangeSliders(
                min, max,
                minValue, maxValue,
                minId, maxId,
                width = 95f
            )

            val mn = minValue.get()
            val mx = maxValue.get()

            if(mn != prevMin || mx != prevMax) {
                changed()
            }

            prevMin = mn
            prevMax = mx
        }
    }

    override fun thisGet() = arrayOf(minValue.get().toDouble(), maxValue.get().toDouble())

    override fun value(current: CodeGen.Current) = value(
        current, "a Range", GenValue.Range(
            minValue.get().toDouble(),
            maxValue.get().toDouble()
        )
    ) { it is GenValue.Range }

}