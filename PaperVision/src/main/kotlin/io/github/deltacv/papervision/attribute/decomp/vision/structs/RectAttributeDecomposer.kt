package io.github.deltacv.papervision.attribute.decomp.vision.structs

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved

class RectAttributeDecomposer : AttributeDecomposer<RectAttributeDecomposer.Session>() {

    val x = DoubleAttribute(OUTPUT, "X")
    val y = DoubleAttribute(OUTPUT, "Y")
    val width = DoubleAttribute(OUTPUT, "$[att_width]")
    val height = DoubleAttribute(OUTPUT, "$[att_height]")

    override fun onEnable() {
        + x
        + y
        + width
        + height
    }

    override val generators = generatorsBuilder<GenValue, Session> {
        generatorFor(JavaLanguage) {
            assertGenValueType<GenValue.Rect>(genInput)

            val session = Session()

            current.scope {
                when(genInput) {
                    is GenValue.Rect.Components -> {
                        session.x = genInput.x.toRuntime(current)
                        session.y = genInput.y.toRuntime(current)
                        session.width = genInput.w.toRuntime(current)
                        session.height = genInput.h.toRuntime(current)
                    }
                    is GenValue.Rect.Inst -> {
                        val rect = genInput.value.v

                        session.x = GenValue.Double.Runtime(double(rect.propertyValue("x", IntType)).resolved())
                        session.y = GenValue.Double.Runtime(double(rect.propertyValue("y", IntType)).resolved())
                        session.width = GenValue.Double.Runtime(double(rect.propertyValue("width", IntType)).resolved())
                        session.height = GenValue.Double.Runtime(double(rect.propertyValue("height", IntType)).resolved())
                    }
                }
            }

            session
        }
    }

    override fun getGenValueOf(
        current: CodeGen.Current,
        attrib: Attribute
    ) = when(attrib) {
        x -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.x }
        y -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.y }
        width -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.width }
        height -> GenValue.Double.Runtime.defer { current.sessionOf(this)?.height }
        else -> noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var x: GenValue.Double.Runtime
        lateinit var y: GenValue.Double.Runtime
        lateinit var width: GenValue.Double.Runtime
        lateinit var height: GenValue.Double.Runtime
    }
}