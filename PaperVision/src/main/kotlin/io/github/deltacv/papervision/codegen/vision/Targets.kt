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

package io.github.deltacv.papervision.codegen.vision

import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.Visibility
import io.github.deltacv.papervision.codegen.build.ConValue
import io.github.deltacv.papervision.codegen.build.Parameter
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.targets

fun CodeGen.Current.enableTargets() = this {
    if(!codeGen.hasFlag("targetsEnabled")) {
        targets(enableTargetsIfNeeded = false) {
            scope {
                clearTargets()
            }

            group {
                private(targets)
            }

            codeGen.classEndScope {
                val labelParameter = Parameter(JavaTypes.String, "label")
                val rectTargetParameter = Parameter(JvmOpenCvTypes.Rect, "rect")

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "addTarget",
                    labelParameter,
                    rectTargetParameter,
                    isSynchronized = true
                ) {
                    targets("add", TargetType.new(labelParameter, rectTargetParameter))
                }

                separate()

                val rectArrayListTargetsParameter = Parameter(JavaTypes.ArrayList(JvmOpenCvTypes.Rect), "rects")

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "addTargets",
                    labelParameter,
                    rectArrayListTargetsParameter,
                    isSynchronized = true
                ) {
                    foreach(Variable(JvmOpenCvTypes.Rect, "rect"), rectArrayListTargetsParameter) {
                        "addTarget"(labelParameter, it)
                    }
                }

                separate()

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "clearTargets",
                    isSynchronized = true
                ) {
                    targets("clear")
                }

                separate()

                method(Visibility.PUBLIC, TargetType.arrayType(), "getTargets", isSynchronized = true) {
                    val array = Variable("array", TargetType.newArray(targets.callValue("size", IntType)))
                    local(array)

                    separate()

                    ifCondition(targets.callValue("isEmpty", BooleanType).condition()) {
                        returnMethod(array)
                    }

                    separate()

                    forLoop(Variable(IntType, "i"), int(0), targets.callValue("size", IntType) - int(1)) {
                        array.arraySet(it, targets.callValue("get", TargetType, it).castTo(TargetType))
                    }

                    separate()

                    returnMethod(array)
                }

                separate()

                val targetsWithLabel = Variable("targetsWithLabel", JavaTypes.ArrayList(TargetType).new())

                method(Visibility.PUBLIC, JavaTypes.ArrayList(TargetType), "getTargetsWithLabel", labelParameter, isSynchronized = true) {
                    local(targetsWithLabel)

                    separate()

                    foreach(Variable(TargetType, "target"), targets) {
                        val label = it.propertyValue("label", JavaTypes.String)

                        ifCondition(label.callValue("equals", BooleanType, labelParameter).condition()) {
                            targetsWithLabel("add", it)
                        }
                    }

                    separate()

                    returnMethod(targetsWithLabel)
                }

                separate()

                clazz(Visibility.PUBLIC, TargetType.className) {
                    val labelVariable = Variable("label", ConValue(JavaTypes.String, null))
                    val rectVariable = Variable("rect", ConValue(JvmOpenCvTypes.Rect, null))

                    instanceVariable(Visibility.PUBLIC, labelVariable, isFinal = true)
                    instanceVariable(Visibility.PUBLIC, rectVariable, isFinal = true)

                    separate()

                    constructor(Visibility.PROTECTED, TargetType, labelParameter, rectTargetParameter) {
                        labelVariable instanceSet labelParameter
                        rectVariable instanceSet rectTargetParameter
                    }
                }
            }
        }

        codeGen.addFlag("targetsEnabled")
    }
}