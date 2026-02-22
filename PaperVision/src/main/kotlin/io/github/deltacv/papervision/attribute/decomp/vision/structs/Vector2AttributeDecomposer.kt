package io.github.deltacv.papervision.attribute.decomp.vision.structs

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.math.DoubleAttribute
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.Generator
import io.github.deltacv.papervision.codegen.PolyglotMapping
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataReader
import io.github.deltacv.papervision.serialization.v2.DataWriter

@CodecType
class Vector2AttributeDecomposer : AttributeDecomposer<Vector2AttributeDecomposer.Session>() {

    val x = DoubleAttribute(OUTPUT, "X")
    val y = DoubleAttribute(OUTPUT, "Y")

    override fun onEnable() {
        + x
        + y

        if(decomposedAttribute is Vector2Attribute && (decomposedAttribute as Vector2Attribute).useSizeNaming) {
            x.variableName = "$[att_width]"
            y.variableName = "$[att_height]"
        }
    }

    override val generators = generatorsBuilder<GenValue, Session> {
        generatorFor(JavaLanguage) {
            assertGenValueType<GenValue.Vec2>(genInput)

            val session = Session()

            current {
                when(genInput) {
                    is GenValue.Vec2.Actual -> {
                        session.x = genInput.x.toRuntime(current)
                        session.y = genInput.y.toRuntime(current)
                    }
                    is GenValue.Vec2.Runtime -> {
                        session.x = genInput.xValue.toRuntime(current)
                        session.y = genInput.yValue.toRuntime(current)
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
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataWriter) {
        encoder.obj("x", x)
        encoder.obj("y", y)
    }

    override fun decode(decoder: DataReader) {
        decoder.obj("x", x)
        decoder.obj("y", y)
    }

    class Session : CodeGenSession {
        lateinit var x: GenValue.Double.Runtime
        lateinit var y: GenValue.Double.Runtime
    }
}