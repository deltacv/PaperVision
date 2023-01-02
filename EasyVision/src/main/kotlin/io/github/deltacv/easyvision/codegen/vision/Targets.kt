package io.github.deltacv.easyvision.codegen.vision

import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.ConValue
import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Variable
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.OpenCvTypes
import io.github.deltacv.easyvision.codegen.dsl.targets

fun CodeGen.Current.enableTargets() = this {
    if(!codeGen.hasFlag("targetsEnabled")) {
        targets(enableTargetsIfNeeded = false) {
            group {
                private(targets)
            }

            codeGen.classEndScope {
                val labelParameter = Parameter(JavaTypes.String, "label")
                val rectTargetParameter = Parameter(OpenCvTypes.Rect, "rect")

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

                val rectArrayListTargetsParameter = Parameter(JavaTypes.ArrayList(OpenCvTypes.Rect), "rects")

                method(
                    Visibility.PRIVATE,
                    VoidType,
                    "addTargets",
                    labelParameter,
                    rectArrayListTargetsParameter,
                    isSynchronized = true
                ) {
                    foreach(Variable(OpenCvTypes.Rect, "rect"), rectArrayListTargetsParameter) {
                        "addTarget"(labelParameter, it)
                    }
                }

                separate()

                method(Visibility.PUBLIC, JavaTypes.ArrayList(TargetType), "getTargets", isSynchronized = true) {
                    returnMethod(targets.callValue("clone", JavaTypes.ArrayList(TargetType)))
                }

                separate()

                clazz(Visibility.PUBLIC, TargetType.className) {
                    val labelVariable = Variable("label", ConValue(JavaTypes.String, null))
                    val rectVariable = Variable("rect", ConValue(OpenCvTypes.Rect, null))

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