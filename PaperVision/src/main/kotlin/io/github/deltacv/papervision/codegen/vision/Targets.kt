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
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage

fun CodeGen.Current.enableTargets() = this {
    if(!codeGen.hasFlag("targetsEnabled")) {
        targets(enableTargetsIfNeeded = false) {
            scope {
                clearTargets()
            }

            group {
                private(rectTargets)
                private(rotRectTargets)
            }

            codeGen.classEndScope {
                val labelParameter = Parameter(JavaTypes.String, "label")
                val rectTargetParameter = Parameter(JvmOpenCvTypes.Rect, "rect")
                val rotatedRectTargetParameter = Parameter(JvmOpenCvTypes.RotatedRect, "rotRect")

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "clearTargets",
                    isSynchronized = true
                ) {
                    rectTargets("clear")
                    rotRectTargets("clear")
                }

                separate()

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "addRectTarget",
                    labelParameter,
                    rectTargetParameter,
                    isSynchronized = true
                ) {
                    rectTargets("put", labelParameter, rectTargetParameter)
                }

                method(
                    Visibility.PRIVATE,
                    VoidType, "addRotRectTarget",
                    labelParameter,
                    rotatedRectTargetParameter,
                    isSynchronized = true
                ) {
                    rotRectTargets("put", labelParameter, rotatedRectTargetParameter)
                }

                separate()

                method(Visibility.PUBLIC, JvmOpenCvTypes.Rect, "getRectTarget", labelParameter,  isSynchronized = true) {
                    returnMethod(rectTargets.callValue("get", JvmOpenCvTypes.Rect, labelParameter).castTo(JvmOpenCvTypes.Rect))
                }

                separate()

                method(Visibility.PUBLIC, JavaTypes.List(JvmOpenCvTypes.Rect), "getRectTargets", labelParameter,  isSynchronized = true) {
                    // get all the rect targets that start with label
                    val targets = Variable("targets", JavaTypes.ArrayList(JvmOpenCvTypes.Rect).new())
                    local(targets)

                    separate()

                    foreach(Variable(JavaTypes.Map.Entry(JavaTypes.String, JvmOpenCvTypes.Rect), "namedTarget"), rectTargets.callValue("entrySet", JavaTypes.Set(JavaTypes.Map.Entry(JvmOpenCvTypes.Rect, JavaTypes.String)))) {
                        ifCondition (it.callValue("getKey", JavaTypes.String).castTo(JavaTypes.String).callValue("startsWith", BooleanType, labelParameter).condition()) {
                            targets("add", it.callValue("getValue", JvmOpenCvTypes.Rect))
                        }
                    }

                    separate()

                    returnMethod(targets)
                }

                separate()

                method(Visibility.PUBLIC, JvmOpenCvTypes.RotatedRect, "getRotRectTarget", labelParameter, isSynchronized = true) {
                    returnMethod(rotRectTargets.callValue("get", JvmOpenCvTypes.RotatedRect, labelParameter).castTo(JvmOpenCvTypes.RotatedRect))
                }

                separate()

                method(Visibility.PUBLIC, JavaTypes.List(JvmOpenCvTypes.RotatedRect), "getRotRectTargets", labelParameter, isSynchronized = true) {
                    // get all the rect targets that start with label
                    val targets = Variable("targets", JavaTypes.ArrayList(JvmOpenCvTypes.RotatedRect).new())
                    local(targets)

                    separate()

                    foreach(Variable(JavaTypes.Map.Entry(JavaTypes.String, JvmOpenCvTypes.RotatedRect), "namedTarget"), rotRectTargets.callValue("entrySet", JavaTypes.Set(JavaTypes.Map.Entry(JvmOpenCvTypes.RotatedRect, JavaTypes.String)))) {
                        ifCondition (it.callValue("getKey", JavaTypes.String).castTo(JavaTypes.String).callValue("startsWith", BooleanType, labelParameter).condition()) {
                            targets("add", it.callValue("getValue", JvmOpenCvTypes.RotatedRect))
                        }
                    }

                    separate()

                    returnMethod(targets)
                }

                separate()
            }
        }

        codeGen.addFlag("targetsEnabled")
    }
}