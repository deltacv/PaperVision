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

package org.deltacv.papervision.codegen.build.language.jvm

import org.deltacv.papervision.codegen.CodeGen
import org.deltacv.papervision.codegen.Visibility
import org.deltacv.papervision.codegen.build.AccessorVariable
import org.deltacv.papervision.codegen.build.Parameter
import org.deltacv.papervision.codegen.build.DeclarableVariable
import org.deltacv.papervision.codegen.dsl.jvm.jvmTargets

fun CodeGen.Current.enableJavaTargets() = this {
    if(!codeGen.hasFlag("targetsEnabled")) {
        jvmTargets(enableTargetsIfNeeded = false) {
            scope {
                clearTargets()
            }

            group {
                private(rectTargets)
                private(rotRectTargets)
            }

            codeGen.classEndScope {
                val labelParameter = Parameter(JavaTypes.String, "label")
                val rectTargetParameter = Parameter(JvmOpenCv.Rect, "rect")
                val rotatedRectTargetParameter = Parameter(JvmOpenCv.RotatedRect, "rotRect")

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

                method(Visibility.PUBLIC, JvmOpenCv.Rect, "getRectTarget", labelParameter,  isSynchronized = true) {
                    returnMethod(rectTargets.callValue("get", JvmOpenCv.Rect, labelParameter).castTo(JvmOpenCv.Rect))
                }

                separate()

                method(Visibility.PUBLIC, JavaTypes.List(JvmOpenCv.Rect), "getRectTargets", labelParameter,  isSynchronized = true) {
                    // get all the rect targets that start with label
                    val targets = DeclarableVariable("targets", JavaTypes.ArrayList(JvmOpenCv.Rect).new())
                    local(targets)

                    separate()

                    val entryType = JavaTypes.Map.Entry(JavaTypes.String, JvmOpenCv.Rect)
                    val entrySetType = JavaTypes.Set(entryType) // oof
                    foreach(AccessorVariable(entryType, "namedTarget"), rectTargets.callValue("entrySet", entrySetType)) {
                        ifCondition (it.callValue("getKey", JavaTypes.String).castTo(JavaTypes.String).callValue("startsWith", BooleanType, labelParameter).condition()) {
                            targets("add", it.callValue("getValue", JvmOpenCv.Rect))
                        }
                    }

                    separate()

                    returnMethod(targets)
                }

                separate()

                method(Visibility.PUBLIC, JvmOpenCv.RotatedRect, "getRotRectTarget", labelParameter, isSynchronized = true) {
                    returnMethod(rotRectTargets.callValue("get",
                        JvmOpenCv.RotatedRect, labelParameter).castTo(JvmOpenCv.RotatedRect))
                }

                separate()

                method(Visibility.PUBLIC, JavaTypes.List(JvmOpenCv.RotatedRect), "getRotRectTargets", labelParameter, isSynchronized = true) {
                    // get all the rect targets that start with label
                    val targets = DeclarableVariable("targets", JavaTypes.ArrayList(JvmOpenCv.RotatedRect).new())
                    local(targets)

                    separate()

                    val entryType = JavaTypes.Map.Entry(JavaTypes.String, JvmOpenCv.RotatedRect)
                    val entrySetType = JavaTypes.Set(entryType) // oof

                    foreach(DeclarableVariable(entryType, "namedTarget"), rotRectTargets.callValue("entrySet", entrySetType)) {
                        ifCondition (it.callValue("getKey", JavaTypes.String).castTo(JavaTypes.String).callValue("startsWith", BooleanType, labelParameter).condition()) {
                            targets("add", it.callValue("getValue", JvmOpenCv.RotatedRect))
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



