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
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.Category

/*
@PaperNode(
    name = "nod_sumintegers",
    category = Category.MATH,
    description = "Sums a list of integers and outputs the result"
)*/
class SumIntegerNode : DrawNode<SumIntegerNode.Session>() {

    val numbers = ListAttribute(INPUT, IntAttribute, "$[att_numbers]")
    val result  = IntAttribute(OUTPUT, "$[att_result]")

    override fun onEnable() {
        + numbers
        + result
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == result) {
            return lastGenSession!!.result
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Int
    }

}