package io.github.deltacv.papervision.codegen

import io.github.deltacv.papervision.attribute.Attribute

interface GenValueMapper {
    fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue
}