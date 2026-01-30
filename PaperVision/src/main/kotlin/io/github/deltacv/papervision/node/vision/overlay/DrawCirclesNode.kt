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
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.CircleAttribute
import io.github.deltacv.papervision.attribute.vision.structs.LineParametersAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.AccessorVariable
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_drawcircles",
    category = Category.OVERLAY,
    description = "des_drawcircles"
)
open class DrawCirclesNode : DrawNode<DrawCirclesNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_input]")
    val circles = ListAttribute(INPUT, CircleAttribute, "$[att_circles]")

    val line = LineParametersAttribute(INPUT, "$[att_params]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        +inputMat.rebuildOnChange()

        +line

        +circles.rebuildOnChange()

        +outputMat.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val Circle = JvmOpenCvTypes.getCircleType(current)

                val session = Session()

                val line = line.genValue(current).ensureRuntimeLineJvm(current)

                val input = inputMat.genValue(current)
                val circlesValue = circles.genValue(current) as? GenValue.GList.RuntimeListOf<*>
                    ?: raise("") // TODO: handle non-runtime lists

                val output = uniqueVariable("${input.value.v}Circles", Mat.new())

                group {
                    private(output)
                }

                current.scope {
                    nameComment()

                    input.value.v("copyTo", output)

                    foreach(AccessorVariable(Circle, "circle"), circlesValue.value.v) {
                        JvmOpenCvTypes.Imgproc(
                            "circle",
                            output,
                            it.propertyValue("center", JvmOpenCvTypes.Point),
                            int(it.propertyValue("radius", FloatType)),
                            line.colorScalarValue.v,
                            line.thicknessValue.v
                        )
                    }

                    outputMat.streamIfEnabled(output, input.color)
                }

                session.outputMat = GenValue.Mat(output.resolved(), input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val input = inputMat.genValue(current)
                val circlesValue = circles.genValue(current) as? GenValue.GList.RuntimeListOf<*>
                    ?: raise("") // TODO: handle non-runtime lists

                val line = line.genValue(current)
                if (line !is GenValue.LineParameters.Line) {
                    raise("Line parameters must not be runtime")
                }

                current.scope {
                    nameComment()

                    val output = uniqueVariable("${input.value.v}_circles",
                        input.value.v.callValue("copy", CPythonLanguage.NoType)
                    )
                    local(output)

                    ifCondition(CPythonLanguage.valueIsNot(circlesValue.value.v, CPythonLanguage.NoType)) {
                        foreach(CPythonLanguage.accessorTupleVariable("x", "y", "r"), circlesValue.value.v) {
                            CPythonOpenCvTypes.cv2("circle",
                                output,
                                CPythonLanguage.tuple(int(it.get("x")), int(it.get("y"))),
                                int(it.get("r")),
                                CPythonLanguage.tuple(
                                    line.color.a.value.v,
                                    line.color.b.value.v,
                                    line.color.c.value.v
                                ),
                                line.thickness.value.v
                            )
                        }
                    }

                    session.outputMat = GenValue.Mat(output.resolved(), input.color, input.isBinary)
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if (attrib == outputMat) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMat }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}
