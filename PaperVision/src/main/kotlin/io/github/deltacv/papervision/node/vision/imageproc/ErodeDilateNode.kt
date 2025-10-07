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

package io.github.deltacv.papervision.node.vision.imageproc

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.math.IntAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes.cv2
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode
import io.github.deltacv.papervision.node.vision.ColorSpace
import io.github.deltacv.papervision.util.Range2i

@PaperNode(
    name = "nod_erodedilate",
    category = Category.IMAGE_PROC,
    description = "des_erodedilate"
)
class ErodeDilateNode : DrawNode<ErodeDilateNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_binaryinput]")

    val erodeValue = IntAttribute(INPUT, "$[att_erode]")
    val dilateValue = IntAttribute(INPUT, "$[att_dilate]")

    val outputMat = MatAttribute(OUTPUT, "$[att_output]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()

        + erodeValue
        erodeValue.sliderMode(Range2i(0, 50))

        + dilateValue
        dilateValue.sliderMode(Range2i(0, 50))

        + outputMat.enablePrevizButton().rebuildOnChange()
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            current {
                val session = Session()

                val input = inputMat.genValue(current)
                input.requireBinary(inputMat)

                val erodeVal = erodeValue.genValue(current)
                val erodeValVariable = uniqueVariable("erodeValue", int(erodeVal.value.v))

                val dilateVal = erodeValue.genValue(current)
                val dilateValVariable = uniqueVariable("dilateValue", int(dilateVal.value.v))

                val element = uniqueVariable("element", JvmOpenCvTypes.Mat.nullValue)

                val output = uniqueVariable("${input.value}ErodedDilated", JvmOpenCvTypes.Mat.new())

                group {
                    public(erodeValVariable, erodeValue.label())
                    public(dilateValVariable, dilateValue.label())
                    private(element)
                    private(output)
                }

                current.scope {
                    nameComment()

                    input.value.v("copyTo", output)

                    ifCondition(erodeValVariable greaterThan int(0)) {
                        element instanceSet JvmOpenCvTypes.Imgproc.callValue(
                            "getStructuringElement",
                            JvmOpenCvTypes.Mat,
                            JvmOpenCvTypes.Imgproc.MORPH_RECT,
                            JvmOpenCvTypes.Size.new(erodeValVariable, erodeValVariable)
                        )

                        JvmOpenCvTypes.Imgproc("erode", output, output, element)

                        separate()

                        element("release")
                    }

                    separate()

                    ifCondition(dilateValVariable greaterThan int(0)) {
                        element instanceSet JvmOpenCvTypes.Imgproc.callValue(
                            "getStructuringElement",
                            JvmOpenCvTypes.Mat,
                            JvmOpenCvTypes.Imgproc.MORPH_RECT,
                            JvmOpenCvTypes.Size.new(dilateValVariable, dilateValVariable)
                        )

                        JvmOpenCvTypes.Imgproc("dilate", output, output, element)

                        separate()

                        element("release")
                    }

                    outputMat.streamIfEnabled(output, ColorSpace.GRAY.resolved())
                }

                session.outputMatValue = GenValue.Mat(output.resolved(), input.color, input.isBinary)

                session
            }
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val input = inputMat.genValue(current)
                input.requireBinary(inputMat)

                val erodeVal = erodeValue.genValue(current)
                val dilateVal = dilateValue.genValue(current)

                val output = uniqueVariable(
                    "${input.value}_eroded_dilated",
                    input.value.v.callValue("copy", CPythonLanguage.NoType)
                )

                val elementErode = uniqueVariable("element_erode", cv2.callValue(
                    "getStructuringElement",
                    CPythonLanguage.NoType,
                    cv2.MORPH_RECT,
                    CPythonLanguage.tuple(erodeVal.value.v, erodeVal.value.v)
                ))
                public(elementErode)

                val elementDilate = uniqueVariable("element_dilate", cv2.callValue(
                    "getStructuringElement",
                    CPythonLanguage.NoType,
                    cv2.MORPH_RECT,
                    CPythonLanguage.tuple(dilateVal.value.v, dilateVal.value.v)
                ))
                public(elementDilate)

                current.scope {
                    nameComment()

                    local(output)

                    cv2("erode", output, output, elementErode)
                    cv2("dilate", output, output, elementDilate)
                }

                session.outputMatValue = GenValue.Mat(output.resolved(), input.color, input.isBinary)

                session
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return GenValue.Mat.defer { current.sessionOf(this)?.outputMatValue }
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}