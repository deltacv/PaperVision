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

    open class Mat(val value: Resolvable<Value>, val color: Resolvable<ColorSpace>, val isBinary: Boolean = Boolean.FALSE) : GenValue() {
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

        companion object {
            fun defer(genValueResolver: () -> Mat?) = Mat(
                Resolvable.fromResolvable { genValueResolver()?.value },
                Resolvable.fromResolvable { genValueResolver()?.color },
                Boolean.defer { genValueResolver()?.isBinary }
            )
        }
    }

    sealed class GKeyPoint : GenValue() {
        data class KeyPoint(
            val x: Double,
            val y: Double,
            val size: Double
        ) : GKeyPoint()

        data class RuntimeKeyPoint(val value: Resolvable<Value>) : GKeyPoint() {
            companion object {
                fun defer(genValueResolver: () -> RuntimeKeyPoint?) = RuntimeKeyPoint(
                    Resolvable.fromResolvable { genValueResolver()?.value }
                )
            }
        }
    }

    data class Point(val x: Resolvable<Double>, val y: Resolvable<Double>) : GenValue()

    sealed class GPoints : GenValue() {
        data class Points(val points: Resolvable<Array<Point>>) : GPoints()
        data class RuntimePoints(val value: Resolvable<Value>) : GPoints() {
            companion object {
                fun defer(genValueResolver: () -> RuntimePoints?) = RuntimePoints(
                    Resolvable.fromResolvable { genValueResolver()?.value }
                )
            }
        }
    }

    sealed class GCircle : GenValue() {
        data class Circle(val x: Double, val y: Double,  val r: Double) : GCircle()

        data class RuntimeCircle(val value: Resolvable<Value>) : GCircle() {
            companion object {
                fun defer(genValueResolver: () -> RuntimeCircle?) = RuntimeCircle(
                    Resolvable.fromResolvable { genValueResolver()?.value }
                )
            }
        }
    }

    sealed class GRect : GenValue() {
        data class Rect(val x: Double, val y: Double, val w: Double, val h: Double) : GRect()

        data class RuntimeRect(val value: Resolvable<Value>) : GRect() {
            companion object {
                fun defer(genValueResolver: () -> RuntimeRect?) = RuntimeRect(
                    Resolvable.fromResolvable { genValueResolver()?.value }
                )
            }
        }

        sealed class Rotated : GRect() {
            data class RotatedRect(
                val x: Double, val y: Double,
                val w: Double, val h: Double,
                val angle: Double
            ) : Rotated()

            data class RuntimeRotatedRect(val value: Resolvable<Value>) : Rotated() {
                companion object {
                    fun defer(genValueResolver: () -> RuntimeRotatedRect?) = RuntimeRotatedRect(
                        Resolvable.fromResolvable { genValueResolver()?.value }
                    )
                }
            }
        }
    }

    data class Enum<E : kotlin.Enum<E>>(val value: E) : GenValue()

    data class Int(val value: Resolvable<kotlin.Int>) : GenValue(){
        companion object {
            val ZERO = Int(Resolvable.Now(0))

            fun defer(genValueResolver: () -> Int?) = Int(
                Resolvable.fromResolvable { genValueResolver()?.value }
            )
        }
    }
    data class Float(val value: Resolvable<kotlin.Float>) : GenValue() {
        companion object {
            val ZERO = Float(Resolvable.Now(0.0f))

            fun defer(genValueResolver: () -> Float?) = Float(
                Resolvable.fromResolvable { genValueResolver()?.value }
            )
        }
    }
    data class Double(val value: Resolvable<kotlin.Double>) : GenValue() {
        companion object {
            val ZERO = Double(Resolvable.Now(0.0))

            fun defer(genValueResolver: () -> Double?) = Double(
                Resolvable.fromResolvable { genValueResolver()?.value }
            )
        }
    }

    data class String(val value: Resolvable<kotlin.String>) : GenValue()

    sealed class LineParameters : GenValue() {
        data class Line(val color: Scalar, val thickness: Int) : LineParameters() {
            companion object {
                fun defer(genValueResolver: () -> Line?) = Line(
                    Scalar.defer { genValueResolver()?.color },
                    Int.defer { genValueResolver()?.thickness }
                )
            }
        }
        data class RuntimeLine(val colorScalarValue: Resolvable<Value>, val thicknessValue: Resolvable<Value>) : LineParameters() {
            companion object {
                fun defer(genValueResolver: () -> RuntimeLine?) = RuntimeLine(
                    Resolvable.fromResolvable { genValueResolver()?.colorScalarValue },
                    Resolvable.fromResolvable { genValueResolver()?.thicknessValue }
                )
            }
        }

        fun ensureRuntimeLineJvm(current: CodeGen.Current): RuntimeLine {
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

            fun defer(genValueResolver: () -> Scalar?) = Scalar(
                Double.defer { genValueResolver()?.a },
                Double.defer { genValueResolver()?.b },
                Double.defer { genValueResolver()?.c },
                Double.defer { genValueResolver()?.d }
            )
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

        companion object {
            fun defer(genValueResolver: () -> Boolean?) = Boolean(
                Resolvable.fromResolvable { genValueResolver()?.value }
            )
        }
    }

    sealed class GList : GenValue() {
        companion object {
            inline fun <reified T : GenValue> RuntimeListOf(value: Resolvable<Value>): RuntimeListOf<T> =
                RuntimeListOf(value, Resolvable.Now(T::class))
        }

        open class ListOf<T : GenValue>(val elements: Array<T>) : GList()
        class List(elements: Array<GenValue>) : ListOf<GenValue>(elements)

        data class RuntimeListOf<T : GenValue>(val value: Resolvable<Value>, val typeClass: Resolvable<KClass<T>>) : GList() {
            companion object {
                fun <T : GenValue> defer(
                    genValueResolver: () -> RuntimeListOf<T>?
                ): RuntimeListOf<T> = RuntimeListOf(
                    Resolvable.fromResolvable { genValueResolver()?.value },
                    Resolvable.fromResolvable { genValueResolver()?.typeClass }
                )
            }
        }
    }

    object None : GenValue()

}
