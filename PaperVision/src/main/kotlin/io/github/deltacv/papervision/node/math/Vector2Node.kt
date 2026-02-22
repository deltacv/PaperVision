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

package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder


@PaperNode(
    name = "nod_vector2",
    category = NodeCategory.TRANSFORM,
    description = "des_vector2"
)
@CodecType
class Vector2Node @JvmOverloads constructor(
    useSizeNaming: Boolean = false
) : DrawNode<Vector2Node.Session>() {

    val xAttribute = DoubleAttribute(INPUT, if(useSizeNaming) "att_width" else "X")
    val yAttribute = DoubleAttribute(INPUT, if(useSizeNaming) "att_height" else "Y")

    val result = Vector2Attribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + xAttribute
        + yAttribute

        + result
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                if(codeGen.isForPreviz) {
                    val xValue = xAttribute.genValue(current)
                    val yValue = yAttribute.genValue(current)

                    val x = uniqueVariable("vectorX", if (xValue is GenValue.Double.Actual) xValue.v else double(0.0))
                    val y = uniqueVariable("vectorY", if (yValue is GenValue.Double.Actual) yValue.v else double(0.0))

                    group {
                        public(x, xAttribute.label())
                        public(y, yAttribute.label())
                    }

                    current.scope {
                        // if runtime, constantly update
                        if (xValue is GenValue.Double.Runtime) {
                            x instanceSet xValue.v
                            y instanceSet yValue.v
                        }

                        // if actual, just set once, don't need to bother with constants
                    }

                    session.vector2 = GenValue.Vec2.Runtime(
                        GenValue.Double.Runtime(x.resolved()),
                        GenValue.Double.Runtime(y.resolved())
                    )
                } else {
                    session.vector2 = GenValue.Vec2.wrap(
                        xAttribute.genValue(current).toDouble(current),
                        yAttribute.genValue(current).toDouble(current),
                        current
                    )
                }
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current.scope {
                session.vector2 = GenValue.Vec2.wrap(
                    xAttribute.genValue(current).toDouble(current),
                    yAttribute.genValue(current).toDouble(current),
                    current
                )
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == result) {
            return current.nonNullSessionOf(this).vector2
        }

        noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("xAttribute", xAttribute)
        encoder.obj("yAttribute", yAttribute)
        encoder.obj("result", result)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("xAttribute", xAttribute)
        decoder.obj("yAttribute", yAttribute)
        decoder.obj("result", result)
    }

    class Session : CodeGenSession {
        lateinit var vector2: GenValue.Vec2
    }

}
