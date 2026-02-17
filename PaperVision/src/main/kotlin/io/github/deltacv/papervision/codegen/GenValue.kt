/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.resolve.Resolvable
import io.github.deltacv.papervision.codegen.resolve.from
import io.github.deltacv.papervision.codegen.resolve.resolved
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
                Resolvable.from { genValueResolver()?.value },
                Resolvable.from { genValueResolver()?.color },
                Boolean.defer { genValueResolver()?.isBinary }
            )
        }
    }

    sealed class KeyPoint : GenValue() {
        data class Actual(
            val x: Resolvable<Double>,
            val y: Resolvable<Double>,
            val size: Resolvable<Double>
        ) : GenValue.KeyPoint()

        data class Runtime(val value: Resolvable<Value>) : GenValue.KeyPoint() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }
    }

    data class Point(val x: Resolvable<Double>, val y: Resolvable<Double>) : GenValue()

    sealed class Points : GenValue() {
        data class Actual(val points: Resolvable<Array<Point>>) : Points()
        data class Runtime(val value: Resolvable<Value>) : Points() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }
    }

    sealed class Circle : GenValue() {
        data class Actual(val x: Double, val y: Double, val r: Double) : Circle()

        data class Runtime(val value: Resolvable<Value>) : Circle() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }
    }

    sealed class Rect : GenValue() {
        data class Actual(val x: Double, val y: Double, val w: Double, val h: Double) : Rect()

        data class Runtime(val value: Resolvable<Value>) : Rect() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        sealed class Rotated : Rect() {
            data class Actual(
                val x: Double, val y: Double,
                val w: Double, val h: Double,
                val angle: Double
            ) : Rotated()

            data class Runtime(val value: Resolvable<Value>) : Rotated() {
                companion object {
                    fun defer(genValueResolver: () -> Runtime?) = Runtime(
                        Resolvable.from { genValueResolver()?.value }
                    )
                }
            }
        }
    }

    data class Enum<E : kotlin.Enum<E>>(val value: E) : GenValue()

    sealed class Int : GenValue(){
        data class Actual(val value: Resolvable<kotlin.Int>) : Int() {
            companion object {
                fun defer(genValueResolver: () -> Actual?) = Actual(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        data class Runtime(val value: Resolvable<Value>) : Int() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        companion object {
            val ZERO = Actual(0.resolved())
        }
    }

    sealed class Float : GenValue() {
        data class Actual(val value: Resolvable<kotlin.Float>) : Float() {
            companion object {
                fun defer(genValueResolver: () -> Actual?) = Actual(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        data class Runtime(val value: Resolvable<Value>) : Float() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        companion object {
            val ZERO = Actual(Resolvable.Now(0.0f))
        }
    }

    sealed class Double : GenValue() {
        data class Actual(val value: Resolvable<kotlin.Double>) : Double() {
            companion object {
                fun defer(genValueResolver: () -> Actual?) = Actual(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        data class Runtime(val value: Resolvable<Value>) : Double() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.value }
                )
            }
        }

        companion object {
            val ZERO = Actual(0.0.resolved())
        }
    }

    data class String(val value: Resolvable<kotlin.String>) : GenValue()

    sealed class LineParameters : GenValue() {
        data class Actual(val color: Scalar, val thickness: Int.Actual) : LineParameters() {
            companion object {
                fun defer(genValueResolver: () -> Actual?) = Actual(
                    Scalar.defer { genValueResolver()?.color },
                    Int.Actual.defer { genValueResolver()?.thickness }
                )
            }
        }

        data class Runtime(val colorScalarValue: Resolvable<Value>, val thicknessValue: Int.Runtime) : LineParameters() {
            companion object {
                fun defer(genValueResolver: () -> Runtime?) = Runtime(
                    Resolvable.from { genValueResolver()?.colorScalarValue },
                    Int.Runtime.defer { genValueResolver()?.thicknessValue }
                )
            }
        }

        fun ensureRuntimeLineJvm(current: CodeGen.Current): Runtime {
            return current {
                when (val lineParams = this@LineParameters) {
                    is Actual -> {
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

                        Runtime(Resolvable.Now(color), Int.Runtime(Resolvable.Now(thickness)))
                    }

                    is Runtime -> lineParams
                }
            }
        }
    }

    data class Scalar(
        val a: Double.Actual,
        val b: Double.Actual,
        val c: Double.Actual,
        val d: Double.Actual
    ) : List.Actual<Double.Actual>(listOf(a, b, c, d)) {
        companion object {
            val ZERO = Scalar(Double.ZERO, Double.ZERO, Double.ZERO, Double.ZERO)

            fun defer(genValueResolver: () -> Scalar?) = Scalar(
                Double.Actual.defer { genValueResolver()?.a },
                Double.Actual.defer { genValueResolver()?.b },
                Double.Actual.defer { genValueResolver()?.c },
                Double.Actual.defer { genValueResolver()?.d }
            )
        }
    }

    sealed class Vec2 : GenValue() {
        data class Actual(val x: Double.Actual, val y: Double.Actual) : Vec2()
        data class Runtime(val xValue: Double.Runtime, val yValue: Double.Runtime) : Vec2()

        fun ensureRuntimeVector2Java(current: CodeGen.Current): Runtime {
            return current {
                when (val vec = this@Vec2) {
                    is Actual -> {
                        val x = uniqueVariable("vectorX", vec.x.value.v)
                        val y = uniqueVariable("vectorY", vec.y.value.v)

                        group {
                            public(x)
                            public(y)
                        }

                        Runtime(Double.Runtime(Resolvable.Now(x)), Double.Runtime(Resolvable.Now(y)))
                    }

                    is Runtime -> vec
                }
            }
        }
    }

    data class Range(val min: Double, val max: Double) : GenValue() {
        companion object {
            val ZERO = Range(Double.ZERO, Double.ZERO)
        }
    }

    data class ScalarRange(val a: Range, val b: Range, val c: Range, val d: Range) :
        List.Actual<Range>(listOf(a, b, c, d)) {
        companion object {
            val ZERO = ScalarRange(Range.ZERO, Range.ZERO, Range.ZERO, Range.ZERO)
        }
    }

    open class Boolean(val value: Resolvable<kotlin.Boolean>) : GenValue() {
        object TRUE : Boolean(Resolvable.Now(true))
        object FALSE : Boolean(Resolvable.Now(false))

        companion object {
            fun defer(genValueResolver: () -> Boolean?) = Boolean(
                Resolvable.from { genValueResolver()?.value }
            )
        }
    }

    sealed class List<E: GenValue> : GenValue() {
        companion object {
            inline fun <reified T : GenValue> Runtime(value: Resolvable<Value>): Runtime<T> =
                Runtime(value, Resolvable.Now(T::class))
        }

        fun toActualOrNull(): Actual<E>? {
            return when (this) {
                is Actual -> this
                is Runtime -> null
            }
        }

        open class Actual<E : GenValue>(val elements: kotlin.collections.List<E>) : List<E>()

        data class Runtime<E : GenValue>(val value: Resolvable<Value>, val typeClass: Resolvable<KClass<E>>) : List<E>() {
            companion object {
                fun <E : GenValue> defer(
                    genValueResolver: () -> Runtime<E>?
                ): Runtime<E> = Runtime(
                    Resolvable.from { genValueResolver()?.value },
                    Resolvable.from { genValueResolver()?.typeClass }
                )
            }
        }
    }

    object None : GenValue()

}
