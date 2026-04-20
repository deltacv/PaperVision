package org.deltacv.papervision.codegen.build.language

import org.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import org.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.resolve.Resolvable

object GenPreviz {

    fun toPrevizScalar(
        scalar: GenValue.Scalar,
        labelSource: ScalarAttribute,
        current: CodeGen.Current,
        prefix: String = "scalar"
    ) {
        
    }

    // ------ VEC2 CONVERSION ------

    fun toPrevizVec2(
        vec: GenValue.Vec2,
        labelSource: Vector2Attribute,
        current: CodeGen.Current,
        prefix: String = "vec"
    ) = toPrevizVec2(vec, labelSource.tunerLabel(0), labelSource.tunerLabel(1), current, prefix)

    fun toPrevizVec2(
        vec: GenValue.Vec2,
        firstLabel: String,
        secondLabel: String,
        current: CodeGen.Current,
        prefix: String = "vec",
    ) = current {
        if (codeGen.isForPreviz) {
            val x = uniqueVariable("${prefix}X", vec.x.v, allocateName = true)
            val y = uniqueVariable("${prefix}Y", vec.y.v, allocateName = true)

            deferredGroup(Resolvable.PairPlaceholder(vec.x.isActual.value, vec.y.isActual.value)) {
                if(it.first) public(x, firstLabel)
                if(it.second) public(y, secondLabel)
            }

            GenValue.Vec2.Runtime(
                GenValue.Int.Runtime(Resolvable.DependentPlaceholder(vec.x.isActual.value) {
                    if(it) x else vec.x.v
                }),
                GenValue.Int.Runtime(Resolvable.DependentPlaceholder(vec.y.isActual.value) {
                    if(it) y else vec.y.v
                })
            )
        } else {
            vec
        }
    }

}