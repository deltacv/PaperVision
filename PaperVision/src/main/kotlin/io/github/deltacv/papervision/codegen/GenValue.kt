/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.node.vision.ColorSpace
import kotlin.reflect.KClass

sealed class GenValue {

    data class Mat(val value: Value, val color: ColorSpace, val isBinary: kotlin.Boolean = false) : GenValue() {
        fun requireBinary(attribute: Attribute) {
            attribute.raiseAssert(
                isBinary,
                "Mat is not binary as required, this causes runtime issues."
            )
        }

        fun requireNonBinary(attribute: Attribute) {
            attribute.raiseAssert(
                !isBinary,
                "Mat is binary where it shouldn't be, this causes runtime issues."
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

        data class RuntimeRect(val value: Value) : GRect()

        sealed class Rotated : GRect() {
            data class RotatedRect(val x: Double, val y: Double,
                                   val w: Double, val h: Double,
                                   val angle: Double) : Rotated()
            data class RuntimeRotatedRect(val value: Value) : Rotated()
        }
    }

    data class Enum<E: kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()
    data class Float(val value: kotlin.Float) : GenValue()
    data class Double(val value: kotlin.Double) : GenValue()

    data class String(val value: kotlin.String) : GenValue()

    sealed class LineParameters : GenValue() {
        data class Line(val color: Scalar, val thickness: Int) : LineParameters()
        data class RuntimeLine(val colorScalarValue: Value, val thicknessValue: Value) : LineParameters()

        fun ensureRuntimeLine(current: CodeGen.Current): RuntimeLine {
            return current {
                val lineParams = this@LineParameters

                when(lineParams) {
                    is Line -> {
                        val color = uniqueVariable("lineColor", JvmOpenCvTypes.Scalar.new(
                            lineParams.color.a.v, lineParams.color.b.v, lineParams.color.c.v, lineParams.color.d.v
                        ))

                        val thickness = uniqueVariable("lineThickness", lineParams.thickness.value.v)

                        group {
                            public(color)
                            public(thickness)
                        }

                        RuntimeLine(color, thickness)
                    }
                    is RuntimeLine -> lineParams
                }
            }
        }
    }

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