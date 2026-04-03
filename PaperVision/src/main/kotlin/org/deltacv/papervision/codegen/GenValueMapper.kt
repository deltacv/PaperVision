package org.deltacv.papervision.codegen

import org.deltacv.papervision.attribute.Attribute

interface GenValueMapper {
    fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue
}



