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

package io.github.deltacv.papervision.node.math

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.build.v
import io.github.deltacv.papervision.codegen.dsl.generatorFor
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.Language
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.util.Range2i


@PaperNode(
    name = "nod_vector2",
    category = Category.MATH,
    description = "Composes a Vector in 2D"
)
class Vector2Node : DrawNode<Vector2Node.Session>() {

    val xAttribute = IntAttribute(INPUT, "X")
    val yAttribute = IntAttribute(INPUT, "Y")

    val result = Vector2Attribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + xAttribute
        xAttribute.normalMode(range = Range2i(Int.MIN_VALUE, Int.MAX_VALUE))

        + yAttribute
        yAttribute.normalMode(range = Range2i(Int.MIN_VALUE, Int.MAX_VALUE))

        + result
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val x = uniqueVariable("vectorX", xAttribute.value(current).value.toDouble().v)
                val y = uniqueVariable("vectorY", yAttribute.value(current).value.toDouble().v)

                group {
                    public(x, xAttribute.label())
                    public(y, yAttribute.label())
                }

                session.vector2 = GenValue.Vec2.RuntimeVector2(x, y)
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session.vector2 = GenValue.Vec2.Vector2(xAttribute.value(current).value.toDouble(), yAttribute.value(current).value.toDouble())
            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == result) {
            return current.sessionOf(this)!!.vector2
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var vector2: GenValue.Vec2
    }

}