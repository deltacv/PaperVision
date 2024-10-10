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