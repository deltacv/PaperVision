package io.github.deltacv.easyvision.node.vision.classification.targets

import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.misc.StringAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.RectAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.dsl.targets
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode


@RegisterNode(
    name = "nod_exporttargets",
    category = Category.CLASSIFICATION,
    description = "Export detected targets to return from the pipeline"
)
class ExportTargetsNode : DrawNode<NoSession>() {

    val inputTargets = ListAttribute(INPUT, RectAttribute, "$[att_targets]")
    val label = StringAttribute(INPUT, "$[att_label]")

    override fun onEnable() {
        + inputTargets
        + label
    }

    override fun genCode(current: CodeGen.Current) = current {
        val targetsValue = inputTargets.value(current)

        if(targetsValue !is GenValue.GList.RuntimeListOf<*>) {
            raise("") // TODO: Handle non-runtime lists
        }

        current.targets {
            current.scope {
                addTarget(string(label.value(current).value), targetsValue.value)
            }
        }

        NoSession
    }

}