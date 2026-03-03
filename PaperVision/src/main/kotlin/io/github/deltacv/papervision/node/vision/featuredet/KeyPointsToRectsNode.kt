/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.KeyPointAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.language.jvm.JavaTypes
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage.NoType
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolve.resolved
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_keypointsto_rects",
    category = NodeCategory.TRANSFORM,
    description = "des_keypointsto_rects"
)
@CodecType
class KeyPointsToRectsNode : DrawNode<KeyPointsToRectsNode.Session>() {

    val input = ListAttribute(INPUT, "$[att_keypoints]", KeyPointAttribute)
    val output = ListAttribute(OUTPUT, "$[att_rects]", RectAttribute)

    override fun onEnable() {
        + input.rebuildOnChange()
        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                input.requireAttachedAttribute()
                val keypoints = input.genValue(current)

                if(keypoints !is GenValue.List.Runtime<*>) {
                    raise("Only runtime lists are supported for now")
                }

                val rects = uniqueVariable("keypointsRects", JavaTypes.ArrayList(JvmOpenCv.Rect).new())

                group {
                    private(rects)
                }

                current.scope {
                    nameComment()

                    rects("clear")

                    separate()

                    foreach(AccessorVariable(JvmOpenCv.KeyPoint, "kp"), keypoints.value.v.callValue("toArray", JvmOpenCv.KeyPoint.arrayType())) {
                        rects("add", JvmOpenCv.Rect.new(
                            int(it.propertyValue("center", JvmOpenCv.Point).propertyValue("x", DoubleType) - it.propertyValue("size", FloatType) / 2.v),
                            int(it.propertyValue("center", JvmOpenCv.Point).propertyValue("y", DoubleType) - it.propertyValue("size", FloatType) / 2.v),
                            int(it.propertyValue("size", FloatType)),
                            int(it.propertyValue("size", FloatType))
                        ))
                    }

                    session.output = GenValue.List.Runtime<GenValue.Rect.Inst>(rects.resolved())
                }
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            val keypoints = input.genValue(current)

            if(keypoints !is GenValue.List.Runtime<*>) {
                raise("Only runtime lists are supported for now")
            }

            // rectangles in python are just tuples of (x, y, w, h)
            current {
                val rects = uniqueVariable("keypoints_rects", NoType.newArrayOfValues())

                current.scope {
                    local(rects)

                    separate()

                    foreach(DeclarableVariable(NoType, "kp"), keypoints.value.v) {
                        rects("append", CPythonLanguage.tuple(
                            it.propertyValue("pt", NoType)[0.v, NoType] - it.propertyValue("size", NoType) / 2.v,
                            it.propertyValue("pt", NoType)[1.v, NoType] - it.propertyValue("size", NoType) / 2.v,
                            it.propertyValue("size", NoType),
                            it.propertyValue("size", NoType)
                        ))
                    }
                }

                session.output = GenValue.List.Runtime<GenValue.Rect.Inst>(rects.resolved())
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when (attrib) {
            output -> GenValue.List.Runtime.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("input", input)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("input", input)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.List.Runtime<GenValue.Rect.Inst>
    }
}
