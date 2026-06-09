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

package org.deltacv.papervision.node.vision.overlay

import org.deltacv.papervision.attribute.Attribute
import org.deltacv.papervision.attribute.math.IntAttribute
import org.deltacv.papervision.attribute.rebuildOnLink
import org.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import org.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.CodeGenSession
import org.deltacv.papervision.codegen.GenValue
import org.deltacv.papervision.codegen.build.DeclarableVariable
import org.deltacv.papervision.codegen.build.language.GenPreviz
import org.deltacv.papervision.codegen.build.language.jvm.JvmOpenCv
import org.deltacv.papervision.codegen.dsl.polyglot
import org.deltacv.papervision.codegen.language.BaseLanguage
import org.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import org.deltacv.papervision.codegen.resolve.resolved
import org.deltacv.papervision.node.NodeCategory
import org.deltacv.papervision.node.DrawNode
import org.deltacv.papervision.node.PaperNode
import org.deltacv.papervision.node.vision.ColorSpace
import org.deltacv.papervision.serialization.v2.CodecType
import org.deltacv.papervision.serialization.v2.DataDecoder
import org.deltacv.papervision.serialization.v2.DataEncoder

@PaperNode(
    name = "nod_lineparameters",
    category = NodeCategory.TRANSFORM,
    description = "des_lineparameters"
)
@CodecType
class LineParametersNode : DrawNode<LineParametersNode.Session>() {

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")
    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val output = LineParametersAttribute(OUTPUT, "$[att_params]")

    override fun onEnable() {
        + lineColor
        + lineThickness

        lineThickness.value.set(3)

        + output.rebuildOnLink()
    }

    override val generators = polyglot {
        generatorFor<BaseLanguage> {
            val session = Session()

            current {
                val lineColorValue = lineColor.genValue(current)
                val lineThicknessValue = lineThickness.genValue(current)

                val finalLineColorVal = when(lineColorValue) {
                    is GenValue.Scalar.Inst -> lineColorValue.value.v
                    is GenValue.Scalar.Components -> {
                        val lineColorVar = uniqueVariable("lineColor", JvmOpenCv.Scalar(lineColorValue, current))

                        group {
                            public(lineColorVar, lineColor.tunerLabel())
                        }
                        JvmOpenCv.syncScalarVariable(lineColorVar, lineColorValue, current)

                        lineColorVar
                    }
                }
                val lineThicknessVar = GenPreviz.toPrevizInt(
                    lineThicknessValue, lineThickness, current,
                    variableName = "lineThickness"
                )

                session.lineParameters = GenValue.LineParameters.Runtime(GenValue.Scalar.Inst(finalLineColorVal.resolved()), lineThicknessVar.toRuntime(current))
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session.lineParameters = GenValue.LineParameters.wrap(
                lineColor.genValue(current),
                lineThickness.genValue(current),
                current
            )

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> current.nonNullSessionOf(this).lineParameters
            else -> noValue(attrib)
        }
    }

    override fun encode(encoder: DataEncoder) {
        super.encode(encoder)
        encoder.obj("lineColor", lineColor)
        encoder.obj("lineThickness", lineThickness)
        encoder.obj("output", output)
    }

    override fun decode(decoder: DataDecoder) {
        super.decode(decoder)
        decoder.obj("lineColor", lineColor)
        decoder.obj("lineThickness", lineThickness)
        decoder.obj("output", output)
    }

    class Session : CodeGenSession {
        lateinit var lineParameters: GenValue.LineParameters
    }

}



