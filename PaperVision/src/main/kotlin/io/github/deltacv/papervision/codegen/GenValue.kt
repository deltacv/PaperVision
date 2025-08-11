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
import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.GenPlaceholderValue
import io.github.deltacv.papervision.codegen.build.Type
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import kotlin.reflect.KClass

sealed class GenValue {

    abstract class SingleValGenValue : GenValue() {
        abstract val value: Value
    }
    abstract class DoubleValGenValue : GenValue() {
        abstract val value: Value
    }

    open class Mat(override val value: Value, val color: ColorSpace, val isBinary: kotlin.Boolean = false) : SingleValGenValue() {
        open fun requireBinary(attribute: Attribute) {
            if(value is GenPlaceholderValue<*>) {
                val placeholderValue = value as GenPlaceholderValue<*>
                placeholderValue.resolver.onResolve {
                    val genValue = placeholderValue.resolver.genValueResolver()

                    if (genValue is Mat) {
                        attribute.raiseAssert(
                            genValue.isBinary,
                            "Mat is not binary as required, this causes runtime issues."
                        )
                    } else {
                        attribute.raiseAssert(
                            false,
                            "Placeholder value does not resolve to a Mat."
                        )
                    }
                }
            } else {
                attribute.raiseAssert(
                    isBinary,
                    "Mat is not binary as required, this causes runtime issues."
                )
            }
        }

        open fun requireNonBinary(attribute: Attribute) {
            if(value is GenPlaceholderValue<*>) {
                val placeholderValue = value as GenPlaceholderValue<*>

                placeholderValue.resolver.onResolve {
                    val genValue = placeholderValue.resolver.genValueResolver()

                    if (genValue is Mat) {
                        attribute.raiseAssert(
                            !genValue.isBinary,
                            "Mat is binary, but non-binary was required."
                        )
                    } else {
                        attribute.raiseAssert(
                            false,
                            "Placeholder value does not resolve to a Mat."
                        )
                    }
                }
            } else {
                attribute.raiseAssert(
                    !isBinary,
                    "Mat is binary, but non-binary was required."
                )
            }
        }
    }

    data class Point(val x: Double, val y: Double) : GenValue()

    sealed class GPoints : GenValue() {
        class Points(val points: Array<Point>) : GPoints()
        data class RuntimePoints(val value: Value) : GPoints()
    }

    sealed class GRect : GenValue() {
        data class Rect(val x: Double, val y: Double, val w: Double, val h: Double) : GRect()

        data class RuntimeRect(val value: Value) : GRect()

        sealed class Rotated : GRect() {
            data class RotatedRect(
                val x: Double, val y: Double,
                val w: Double, val h: Double,
                val angle: Double
            ) : Rotated()

            data class RuntimeRotatedRect(val value: Value) : Rotated()
        }
    }

    data class Enum<E : kotlin.Enum<E>>(val value: E, val clazz: Class<*>) : GenValue()

    data class Int(val value: kotlin.Int) : GenValue()
    data class Float(val value: kotlin.Float) : GenValue()
    data class Double(val value: kotlin.Double) : GenValue()

    data class String(val value: kotlin.String) : GenValue()

    data class ColorSpace(override val value: Value) : SingleValGenValue() {
        constructor(colorSpace: io.github.deltacv.papervision.node.vision.ColorSpace) : this(ConValue(Type.NONE, colorSpace.name))
    }

    sealed class LineParameters : GenValue() {
        data class Line(val color: Scalar, val thickness: Int) : LineParameters()
        data class RuntimeLine(val colorScalarValue: Value, val thicknessValue: Value) : LineParameters()

        fun ensureRuntimeLineJava(current: CodeGen.Current): RuntimeLine {
            return current {
                when (val lineParams = this@LineParameters) {
                    is Line -> {
                        val color = uniqueVariable(
                            "lineColor", JvmOpenCvTypes.Scalar.new(
                                lineParams.color.a.v, lineParams.color.b.v, lineParams.color.c.v, lineParams.color.d.v
                            )
                        )

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
    ) : GList.ListOf<GenValue.Double>(arrayOf(Double(a), Double(b), Double(c), Double(d))) {
        companion object {
            val ZERO = Scalar(0.0, 0.0, 0.0, 0.0)
        }
    }

    sealed class Vec2 : GenValue() {
        data class Vector2(val x: kotlin.Double, val y: kotlin.Double) : Vec2()
        data class RuntimeVector2(val xValue: Value, val yValue: Value) : Vec2()

        fun ensureRuntimeVector2Java(current: CodeGen.Current): RuntimeVector2 {
            return current {
                when (val vec = this@Vec2) {
                    is Vector2 -> {
                        val x = uniqueVariable("vectorX", vec.x.v)
                        val y = uniqueVariable("vectorY", vec.y.v)

                        group {
                            public(x)
                            public(y)
                        }

                        RuntimeVector2(x, y)
                    }

                    is RuntimeVector2 -> vec
                }
            }
        }
    }

    data class Range(val min: kotlin.Double, val max: kotlin.Double) : GenValue() {
        companion object {
            val ZERO = Range(0.0, 0.0)
        }
    }

    data class ScalarRange(val a: Range, val b: Range, val c: Range, val d: Range) :
        GList.ListOf<Range>(arrayOf(a, b, c, d)) {
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