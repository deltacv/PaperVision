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

package org.deltacv.papervision.node.math

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.DoubleAttribute
import org.deltacv.papervision.attribute.math.IntAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.language.GenPreviz
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_integerto_decimal",
    category = NodeCategory.MATH,
    description = "des_integerto_decimal"
)
@CodecType
class IntegerToDecimalNode : DrawNode<IntegerToDecimalNode.Session>(){

    val input = IntAttribute(INPUT, "$[att_input]")
    val output = DoubleAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + input
        + output
    }

    override val generators = polyglot {
        generatorForAny {
            val session = Session()

            val inputValue = input.genValue(current)

            current {
                var inputV = GenPreviz.toPrevizInt(
                    inputValue, input, current, variableName = "integerToDecimalTarget"
                ).v

                session.output = GenValue.Double.Runtime(double(inputV).resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute) = when(attrib) {
        output -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.output }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.Double.Runtime
    }
}



