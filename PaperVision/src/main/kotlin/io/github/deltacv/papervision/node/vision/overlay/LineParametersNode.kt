/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.node.vision.overlay

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import io.github.deltacv.papervision.attribute.vision.structs.ScalarAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace

@PaperNode(
    name = "nod_lineparameters",
    category = Category.OVERLAY,
    description = "des_lineparameters"
)
class LineParametersNode : DrawNode<LineParametersNode.Session>() {

    val lineColor = ScalarAttribute(INPUT, ColorSpace.RGB, "$[att_linecolor]")
    val lineThickness = IntAttribute(INPUT, "$[att_linethickness]")

    val output = LineParametersAttribute(OUTPUT, "$[att_params]")

    override fun onEnable() {
        + lineColor
        + lineThickness

        lineThickness.value.set(3)

        + output.rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val lineColorValue = lineColor.value(current)

                val lineColorVar = uniqueVariable("lineColor", JvmOpenCvTypes.Scalar.new(
                    lineColorValue.a.value.v, lineColorValue.b.value.v, lineColorValue.c.value.v, lineColorValue.d.value.v
                ))

                val lineThicknessVar = uniqueVariable("lineThickness", lineThickness.value(current).value.v)

                group {
                    public(lineColorVar, lineColor.label())
                    public(lineThicknessVar, lineThickness.label())
                }

                session.lineParameters = GenValue.LineParameters.RuntimeLine(lineColorVar.resolved(), lineThicknessVar.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session.lineParameters = GenValue.LineParameters.Line(
                lineColor.value(current),
                lineThickness.value(current)
            )

            session
        }
    }
    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        val lp = current.sessionOf(this)?.lineParameters

        return when (current.language) {
            // select the appropriate type based on the language;
            // Java always uses runtime line parameters
            // CPython always uses the static line parameters
            is JavaLanguage   -> GenValue.LineParameters.RuntimeLine.defer { lp as? GenValue.LineParameters.RuntimeLine }
            is CPythonLanguage -> GenValue.LineParameters.Line.defer { lp as? GenValue.LineParameters.Line }
            else -> null
        } ?: noValue(attrib)
    }


    class Session : CodeGenSession {
        lateinit var lineParameters: GenValue.LineParameters
    }

}