package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.node.vision.ColorSpace
import kotlin.reflect.KClass

sealed class GenValue {

    data class Mat(val value: Value, val color: ColorSpace, val isBinary: kotlin.Boolean = false) : GenValue() {
        fun requireBinary(attribute: Attribute) {
            attribute.warnAssert(
                isBinary,
                "Input Mat is not binary as required, this might cause runtime issues."
            )
        }

        fun requireNonBinary(attribute: Attribute) {
            attribute.warnAssert(
                !isBinary,
                "Input Mat is binary where it shouldn't be, this might cause runtime issues."
            )
        }
    }

    data class Point(val x: Double, val y: Double) : GenValue()

    sealed class GPoints : GenValue() {
        data class Points(val points: Array<Point>) : GPoints()
        data class RuntimePoints(val value: Value) : GPoints()
    }

    sealed class GRect : GenValue() {
        data class Rect(val x: Double, val y: Double, val w: Double, val h: Double) : GRect()
        data class RotatedRect(val x: Double, val y: Double,
                               val w: Double, val h: Double,
                               val angle: Double) : GRect()

        data class RuntimeRect(val value: Value) : GRect()
        data class RuntimeRotatedRect(val value: Value) : GRect()
    }

    data class Enum<E: kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()
    data class Float(val value: kotlin.Float) : GenValue()
    data class Double(val value: kotlin.Double) : GenValue()

    data class String(val value: kotlin.String) : GenValue()

    data class Scalar(
        val a: kotlin.Double,
        val b: kotlin.Double,
        val c: kotlin.Double,
        val d: kotlin.Double
    ) : GList.ListOf<GenValue.Double>(arrayOf(Double(a), Double(b), Double(c), Double(d), )) {
        companion object {
            val ZERO = Scalar(0.0, 0.0, 0.0, 0.0)
        }
    }

    data class Range(val min: kotlin.Double, val max: kotlin.Double) : GenValue(){
        companion object {
            val ZERO = Range(0.0, 0.0)
        }
    }

    data class ScalarRange(val a: Range, val b: Range, val c: Range, val d: Range) : GList.ListOf<Range>(arrayOf(a, b, c, d)) {
        companion object {
            val ZERO = ScalarRange(Range.ZERO, Range.ZERO, Range.ZERO, Range.ZERO)
        }
    }

    sealed class Boolean(val value: kotlin.Boolean) : GenValue() {
        object True : Boolean(true)
        object False : Boolean(false)
    }
    
    sealed class GList : GenValue() {
        open class ListOf<T : GenValue>(val elements: Array<T>) : GList()
        class List(elements: Array<GenValue>) : ListOf<GenValue>(elements)

        data class RuntimeListOf<T : GenValue>(val value: Value, val typeClass: KClass<T>) : GList()
    }

    object None : GenValue()

}