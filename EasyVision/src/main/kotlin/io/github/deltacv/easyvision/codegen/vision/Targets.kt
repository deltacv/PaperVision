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
            scope {
                clearTargets()
            }

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
                    returnMethod(targets.callValue("toArray", JavaTypes.ArrayList(TargetType), TargetType.newArray(int(0))))
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