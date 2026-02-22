package io.github.deltacv.papervision.attribute.decomp.vision.structs

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.decomp.AttributeDecomposer
import io.github.deltacv.papervision.attribute.vision.structs.Vector2Attribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@CodecType
class RectAttributeDecomposer : AttributeDecomposer<RectAttributeDecomposer.Session>() {

    val position = Vector2Attribute(OUTPUT, "$[att_position]")
    val size = Vector2Attribute(OUTPUT, "$[att_size]", useSizeNaming = true)

    override fun onEnable() {
        + position
        + size
    }

    override val generators = generatorsBuilder<GenValue, Session> {
        generatorFor(JavaLanguage) {
            assertGenValueType<GenValue.Rect>(genInput)

            val session = Session()

            current.scope {
                when(genInput) {
                    is GenValue.Rect.Components -> {
                        session.position = GenValue.Vec2.Runtime(
                            genInput.x.toRuntime(current),
                            genInput.y.toRuntime(current)
                        )

                        session.size = GenValue.Vec2.Runtime(
                            genInput.w.toRuntime(current),
                            genInput.h.toRuntime(current)
                        )
                    }
                    is GenValue.Rect.Inst -> {
                        val rect = genInput.value.v

                        val x = GenValue.Double.Runtime(double(rect.propertyValue("x", IntType)).resolved())
                        val y = GenValue.Double.Runtime(double(rect.propertyValue("y", IntType)).resolved())
                        val position = GenValue.Vec2.Runtime(x, y)

                        val width = GenValue.Double.Runtime(double(rect.propertyValue("width", IntType)).resolved())
                        val height = GenValue.Double.Runtime(double(rect.propertyValue("height", IntType)).resolved())
                        val size = GenValue.Vec2.Runtime(width, height)

                        session.position = position
                        session.size = size
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
        position -> GenValue.Vec2.Runtime.defer { current.sessionOf(this)?.position }
        size -> GenValue.Vec2.Runtime.defer { current.sessionOf(this)?.size }
        else -> noValue(attrib)
    }

    override fun encode(encoder: DataEncoder) {
        encoder.obj("position", position)
        encoder.obj("size", size)
    }

    override fun decode(decoder: DataDecoder) {
        decoder.obj("position", position)
        decoder.obj("size", size)
    }

    class Session : CodeGenSession {
        lateinit var position: GenValue.Vec2.Runtime
        lateinit var size: GenValue.Vec2.Runtime
    }
}