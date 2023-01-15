package io.github.deltacv.easyvision.node.vision.classification.targets

import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.misc.StringAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.build.v
import io.github.deltacv.easyvision.codegen.dsl.targets
import io.github.deltacv.easyvision.codegen.vision.enableTargets
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode


@RegisterNode(
    name = "nod_exporttarget",
    category = Category.CLASSIFICATION,
    description = "Export detected target to return from the pipeline"
)
class ExportTargetNode : DrawNode<NoSession>() {

    val inputTarget = RectAttribute(INPUT, "$[att_targets]")
    val label = StringAttribute(INPUT, "$[att_label]")

    override fun onEnable() {
        + inputTarget
        + label
    }

    override fun genCode(current: CodeGen.Current) = current {
        current.targets {
            current.scope {
                addTarget(string(label.value(current).value), inputTarget.value(current).value)
            }
        }

        NoSession
    }

}