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
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.attribute.math.IntAttribute
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
import io.github.deltacv.papervision.util.Range2i


@PaperNode(
    name = "nod_vector2",
    category = NodeCategory.MATH,
    description = "des_vector2"
)
class Vector2Node : DrawNode<Vector2Node.Session>() {

    val xAttribute = IntAttribute(INPUT, "X")
    val yAttribute = IntAttribute(INPUT, "Y")

    val result = Vector2Attribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + xAttribute
        xAttribute.fieldMode(range = Range2i(Int.MIN_VALUE, Int.MAX_VALUE))

        + yAttribute
        yAttribute.fieldMode(range = Range2i(Int.MIN_VALUE, Int.MAX_VALUE))

        + result
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val x = uniqueVariable("vectorX", int(xAttribute.genValue(current)).v)
                val y = uniqueVariable("vectorY", int(yAttribute.genValue(current)).v)

                group {
                    public(x, xAttribute.label())
                    public(y, yAttribute.label())
                }

                session.vector2 = GenValue.Vec2.Runtime(GenValue.Double.Runtime(x.resolved()), GenValue.Double.Runtime(y.resolved()))
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

    class Session : CodeGenSession {
        lateinit var vector2: GenValue.Vec2
    }

}
