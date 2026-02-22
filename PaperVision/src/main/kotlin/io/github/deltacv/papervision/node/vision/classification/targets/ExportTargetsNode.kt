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

package io.github.deltacv.papervision.node.vision.classification.targets

import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.misc.StringAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RotatedRectAttribute
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.DeclarableVariable
import io.github.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.dsl.jvm.jvmTargets
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.NodeCategory
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataDecoder
import io.github.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_exportrect_targets",
    category = NodeCategory.CLASSIFICATION,
    description = "des_exportrect_targets"
)
@CodecType
class ExportTargetsNode : DrawNode<NoSession>() {

    val inputTargets = ListAttribute(INPUT, "$[att_targets]", RectAttribute)
    val label = StringAttribute(INPUT, "$[att_label]")

    override fun onEnable() {
        + inputTargets
        + label
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val targetsValue = inputTargets.genValue(current)

                if(targetsValue !is GenValue.List.Runtime<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val labelValue = label.genValue(current).value

                labelValue.letOrDefer {
                    if(it.isEmpty()) {
                        label.raise("err_emptylabel")
                    }
                }

                current.jvmTargets {
                    current.scope {
                        forLoop(AccessorVariable(IntType, "i"), 0.v, targetsValue.value.v.callValue("size", IntType), 1.v) {
                            addRectTarget(string("${labelValue}_").plus(it), targetsValue.value.v.callValue("get", JvmOpenCv.Rect, it).castTo(JvmOpenCv.Rect))
                        }
                    }
                }

                NoSession
            }
        }

        generatorFor(CPythonLanguage) {
            warn("err_targetsnot_supported")
            NoSession
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("inputTarget", inputTargets)
        encoder.obj("label", label)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("inputTarget", inputTargets)
        decoder.obj("label", label)
    }

}


@PaperNode(
    name = "nod_exportrot_recttargets",
    category = NodeCategory.CLASSIFICATION,
    description = "des_exportrot_recttargets"
)
class ExportRotTargetsNode : DrawNode<NoSession>() {

    val inputTargets = ListAttribute(INPUT, "$[att_targets]", RotatedRectAttribute)
    val label = StringAttribute(INPUT, "$[att_label]")

    override fun onEnable() {
        + inputTargets
        + label
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val targetsValue = inputTargets.genValue(current)

                if(targetsValue !is GenValue.List.Runtime<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                val labelValue = label.genValue(current).value
                labelValue.letOrDefer {
                    if(it.isEmpty()) {
                        label.raise("err_emptylabel")
                    }
                }

                current.jvmTargets {
                    current.scope {
                        forLoop(DeclarableVariable(IntType, "i"), 0.v, targetsValue.value.v.callValue("size", IntType), 1.v) {
                            addRotRectTarget(string("${labelValue}_").plus(it), targetsValue.value.v.callValue("get", JvmOpenCv.RotatedRect, it).castTo(JvmOpenCv.RotatedRect))
                        }
                    }
                }

                NoSession
            }
        }

        generatorFor(CPythonLanguage) {
            warn("err_targetsnot_supported")
            NoSession
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("inputTarget", inputTargets)
        encoder.obj("label", label)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("inputTarget", inputTargets)
        decoder.obj("label", label)
    }

}
