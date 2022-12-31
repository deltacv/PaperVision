package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.Category

@RegisterNode(
    name = "nod_sumintegers",
    category = Category.MATH,
    description = "Sums a list of integers and outputs the result"
)
class SumIntegerNode : DrawNode<SumIntegerNode.Session>() {

    val numbers = ListAttribute(INPUT, IntAttribute, "$[att_numbers]", sameLine = true)
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