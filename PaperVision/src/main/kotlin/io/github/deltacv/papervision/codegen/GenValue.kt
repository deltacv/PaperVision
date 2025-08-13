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

    open class Mat(val value: Resolvable<Value>, val color: Resolvable<ColorSpace>, val isBinary: Boolean = Boolean.TRUE) : GenValue() {
        open fun requireBinary(attribute: Attribute) {
            isBinary.value.letOrDefer {
                attribute.raiseAssert(it, "Mat is not binary as required, this causes runtime issues.")
            }
        }

        open fun requireNonBinary(attribute: Attribute) {
            isBinary.value.letOrDefer {
                attribute.raiseAssert(!it, "Mat is binary where it shouldn't be, this causes runtime issues.")
            }
        }
    }

    data class Point(val x: Resolvable<Double>, val y: Resolvable<Double>) : GenValue()

    sealed class GPoints : GenValue() {
        class Points(val points: Resolvable<Array<Point>>) : GPoints()
        data class RuntimePoints(val value: Resolvable<Value>) : GPoints()
    }

    sealed class GRect : GenValue() {
        data class Rect(val x: Resolvable<Double>, val y: Resolvable<Double>, val w: Resolvable<Double>, val h: Resolvable<Double>) : GRect()

        data class RuntimeRect(val value: Resolvable<Value>) : GRect()

        sealed class Rotated : GRect() {
            data class RotatedRect(
                val x: Resolvable<Double>, val y: Resolvable<Double>,
                val w: Resolvable<Double>, val h: Resolvable<Double>,
                val angle: Resolvable<Double>
            ) : Rotated()

            data class RuntimeRotatedRect(val value: Resolvable<Value>) : Rotated()
        }
    }

    data class Enum<E : kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: Resolvable<kotlin.Int>) : GenValue(){
        companion object {
            val ZERO = Int(Resolvable.Now(0))
        }
    }
    data class Float(val value: Resolvable<kotlin.Float>) : GenValue() {
        companion object {
            val ZERO = Float(Resolvable.Now(0.0f))
        }
    }
    data class Double(val value: Resolvable<kotlin.Double>) : GenValue() {
        companion object {
            val ZERO = Double(Resolvable.Now(0.0))
        }
    }

    data class String(val value: Resolvable<kotlin.String>) : GenValue()

    sealed class LineParameters : GenValue() {
        data class Line(val color: Scalar, val thickness: Int) : LineParameters()
        data class RuntimeLine(val colorScalarValue: Resolvable<Value>, val thicknessValue: Resolvable<Value>) : LineParameters()

        fun ensureRuntimeLineJava(current: CodeGen.Current): RuntimeLine {
            return current {
                when (val lineParams = this@LineParameters) {
                    is Line -> {
                        val color = uniqueVariable(
                            "lineColor", JvmOpenCvTypes.Scalar.new(
                                lineParams.color.a.value.v,
                                lineParams.color.b.value.v,
                                lineParams.color.c.value.v,
                                lineParams.color.d.value.v
                            )
                        )

                        val thickness = uniqueVariable("lineThickness", lineParams.thickness.value.v)

                        group {
                            public(color)
                            public(thickness)
                        }

                        RuntimeLine(Resolvable.Now(color), Resolvable.Now(thickness))
                    }

                    is RuntimeLine -> lineParams
                }
            }
        }
    }

    data class Scalar(
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double
    ) : GList.ListOf<GenValue.Double>(arrayOf(a, b, c, d)) {
        companion object {
            val ZERO = Scalar(Double.ZERO, Double.ZERO, Double.ZERO, Double.ZERO)
        }
    }

    sealed class Vec2 : GenValue() {
        data class Vector2(val x: Double, val y: Double) : Vec2()
        data class RuntimeVector2(val xValue: Resolvable<Value>, val yValue: Resolvable<Value>) : Vec2()

        fun ensureRuntimeVector2Java(current: CodeGen.Current): RuntimeVector2 {
            return current {
                when (val vec = this@Vec2) {
                    is Vector2 -> {
                        val x = uniqueVariable("vectorX", vec.x.value.v)
                        val y = uniqueVariable("vectorY", vec.y.value.v)

                        group {
                            public(x)
                            public(y)
                        }

                        RuntimeVector2(Resolvable.Now(x), Resolvable.Now(y))
                    }

                    is RuntimeVector2 -> vec
                }
            }
        }
    }

    data class Range(val min: Double, val max: Double) : GenValue() {
        companion object {
            val ZERO = Range(Double(Resolvable.Now(0.0)), Double(Resolvable.Now(0.0)))
        }
    }

    data class ScalarRange(val a: Range, val b: Range, val c: Range, val d: Range) :
        GList.ListOf<Range>(arrayOf(a, b, c, d)) {
        companion object {
            val ZERO = ScalarRange(Range.ZERO, Range.ZERO, Range.ZERO, Range.ZERO)
        }
    }

    open class Boolean(val value: Resolvable<kotlin.Boolean>) : GenValue() {
        object TRUE : Boolean(Resolvable.Now(true))
        object FALSE : Boolean(Resolvable.Now(false))
    }

    sealed class GList : GenValue() {
        open class ListOf<T : GenValue>(val elements: Array<T>) : GList()
        class List(elements: Array<GenValue>) : ListOf<GenValue>(elements)

        data class RuntimeListOf<T : GenValue>(val value: Resolvable<Value>, val typeClass: KClass<T>) : GList()
    }

    object None : GenValue()

}