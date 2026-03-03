package io.github.deltacv.papervision.attribute.decomp.vision.structs

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@CodecType
class Vector2AttributeDecomposer : AttributeDecomposer<Vector2AttributeDecomposer.Session>() {

    val x = IntAttribute(OUTPUT, "X")
    val y = IntAttribute(OUTPUT, "Y")

    override fun onEnable() {
        +x
        +y

        if (linkedAttribute is Vector2Attribute && (linkedAttribute as Vector2Attribute).useSizeNaming) {
            x.variableName = "$[att_width]"
            y.variableName = "$[att_height]"
        }
    }

    override val generators = generatorsBuilder<GenValue, Session> {
        generatorForAny {
            assertGenValueType<GenValue.Vec2>(genInput)

            val session = Session()

            when (genInput) {
                is GenValue.Vec2.Actual -> {
                    session.x = genInput.x.toRuntime(current)
                    session.y = genInput.y.toRuntime(current)
                }

                is GenValue.Vec2.Runtime -> {
                    session.x = genInput.xValue.toRuntime(current)
                    session.y = genInput.yValue.toRuntime(current)
                }
            }

            session
        }
    }

    override fun getGenValueOf(
        current: CodeGen.Current,
        attrib: Attribute
    ) = when (attrib) {
        x -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.x }
        y -> GenValue.Int.Runtime.defer { current.sessionOf(this)?.y }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        encoder.obj("x", x)
        encoder.obj("y", y)
    }

    override fun decode(decoder: DataDecoder) {
        decoder.obj("x", x)
        decoder.obj("y", y)
    }

    class Session : CodeGenSession {
        lateinit var x: GenValue.Int.Runtime
        lateinit var y: GenValue.Int.Runtime
    }
}